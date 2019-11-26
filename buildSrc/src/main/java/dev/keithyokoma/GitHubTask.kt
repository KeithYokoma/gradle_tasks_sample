package dev.keithyokoma

import com.github.zafarkhaja.semver.Version
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeCommand
import org.eclipse.jgit.api.MergeResult.MergeStatus.CONFLICTING
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

open class GitHubTask : DefaultTask() {

    @Input
    lateinit var token: String

    @TaskAction
    fun merge() {
        val git = buildCurrentGit(project.rootDir)
        val credentialsProvider = UsernamePasswordCredentialsProvider("token", token)

        git.lsRemote()
            .setCredentialsProvider(credentialsProvider)
            .call()
            .filter {
                it.name.contains("release/")
            }.let {
                println("$it")
                it
            }.maxBy {
                Version.valueOf(it.name.replace("refs/heads/release/", ""))
            }!!
            .let { releaseBranch ->
                println(">>> Found branch: ${releaseBranch.name}, Create temp branch")
                val workingBranch = "ci/${releaseBranch.name.replace("refs/heads/release/", "")}_to_master"
                git.createAndCheckoutTo(workingBranch)
                releaseBranch to workingBranch
            }.let { (releaseBranch, workingBranch) ->
                createReleaseBranch(releaseBranch, git, credentialsProvider) to workingBranch
            }.let { (releaseBranch, workingBranch) ->
                println(">>> Merge content of $releaseBranch to $workingBranch")
                mergeNoFFWithBranch(releaseBranch, workingBranch, git, credentialsProvider)
            }.let { branchName ->
                println(">>> Create PR and update assignee")
                GitHub.connectUsingOAuth(token)
                    .getRepository("KeithYokoma/gradle_tasks_sample")
                    .createPullRequest(
                        "[CI] Daily merge $branchName into master",
                        branchName,
                        "master",
                        "Daily merge release into master."
                    )
            }
    }

    private fun buildCurrentGit(rootDir: File) = FileRepositoryBuilder().setGitDir(File("$rootDir/.git"))
        .build()
        .let(::Git)

    private fun createReleaseBranch(branch: Ref, git: Git, provider: CredentialsProvider): Ref {
        git.fetch()
            .setCredentialsProvider(provider)
            .call()

        val branchName = branch.name.replace("refs/heads/", "")
        return git.branchCreate()
            .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
            .setName(branchName)
            .setStartPoint("origin/$branchName")
            .setForce(true)
            .call()
    }

    private fun mergeNoFFWithBranch(branch: Ref, currentBranch: String, git: Git, provider: CredentialsProvider): String {
        val currentFormattedDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
        val mergeResult = git.merge()
            .include(branch)
            .setFastForward(MergeCommand.FastForwardMode.NO_FF)
            .setMessage("CI commit to merge ref ${branch.name} at $currentFormattedDate")
            .call()

        if (!mergeResult.mergeStatus.isSuccessful) {
            val message = mergeResult.toString()
            when (mergeResult.mergeStatus) {
                CONFLICTING -> throw MergeConflictException(message)
                else -> throw error(message)
            }
        }

        git.push()
            .setForce(true)
            .setCredentialsProvider(provider)
            .call()

        return currentBranch
    }

    private class MergeConflictException(message: String) : Exception(message)
}

fun Git.createAndCheckoutTo(branchName: String, delete: Boolean = false): Ref {
    branchDelete().setForce(delete).setBranchNames(branchName).call()
    branchCreate().setName(branchName).call()
    return checkout().setName(branchName).call()
}

fun Git.commitWith(message: String): RevCommit = commit().setAll(true)
    .setMessage(message)
    .call()

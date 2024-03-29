/*
 * Copyright 2011 gitblit.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.gitblit.GitBlit
import com.gitblit.Keys
import com.gitblit.models.RepositoryModel
import com.gitblit.models.UserModel
import com.gitblit.utils.JGitUtils
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.transport.ReceiveCommand
import org.eclipse.jgit.transport.ReceiveCommand.Result
import org.slf4j.Logger

/**
 * Sample Gitblit Post-Receive Hook: jenkins
 *
 * The Post-Receive hook is executed AFTER the pushed commits have been applied
 * to the Git repository.  This is the appropriate point to trigger an
 * integration build or to send a notification.
 * 
 * This script is only executed when pushing to *Gitblit*, not to other Git
 * tooling you may be using.
 * 
 * If this script is specified in *groovy.postReceiveScripts* of gitblit.properties
 * or web.xml then it will be executed by any repository when it receives a
 * push.  If you choose to share your script then you may have to consider
 * tailoring control-flow based on repository access restrictions.
 *
 * Scripts may also be specified per-repository in the repository settings page.
 * Shared scripts will be excluded from this list of available scripts.
 * 
 * This script is dynamically reloaded and it is executed within it's own
 * exception handler so it will not crash another script nor crash Gitblit.
 * 
 * Bound Variables:
 *  gitblit			Gitblit Server	 			com.gitblit.GitBlit
 *  repository		Gitblit Repository			com.gitblit.models.RepositoryModel
 *  user			Gitblit User				com.gitblit.models.UserModel
 *  commands		JGit commands 				Collection<org.eclipse.jgit.transport.ReceiveCommand>
 *	url				Base url for Gitblit		String
 *  logger			Logs messages to Gitblit 	org.slf4j.Logger
 *  clientLogger	Logs messages to Git client	com.gitblit.utils.ClientLogger
 *
 * Accessing Gitblit Custom Fields:
 *   def myCustomField = repository.customFields.myCustomField
 *  
 */
// Indicate we have started the script
logger.info("jenkins hook triggered by ${user.username} for ${repository.name}")

// This script requires Jenkins Git plugin 1.1.14 or later
// http://kohsuke.org/2011/12/01/polling-must-die-triggering-jenkins-builds-from-a-git-hook/

// define your jenkins url here or set groovy.jenkinsServer in 
// gitblit.properties or web.xml
def s = new HashSet()
def jenkinsUrl = gitblit.getString('groovy.jenkinsServer', 'http://jenkins.appfactorypreview.wso2.com:8081')
for (command in commands) {
	def ref = command.refName
	def refType = 'branch'
	if (ref.startsWith('refs/heads/')) {
		ref  = command.refName.substring('refs/heads/'.length())
	} else if (ref.startsWith('refs/tags/')) {
		ref  = command.refName.substring('refs/tags/'.length())
		refType = 'tag'
	}

	switch (command.type) {
		case ReceiveCommand.Type.CREATE:
			break
		case ReceiveCommand.Type.UPDATE:
			s<<ref
			break
		case ReceiveCommand.Type.UPDATE_NONFASTFORWARD:
			ss<<ref
			break
		case ReceiveCommand.Type.DELETE:
		
			break
		default:
			break
	}
}

// define the trigger url
for(branch in s){
def triggerUrl = jenkinsUrl + "/git/notifyCommit?url=$url/git/$repository.name"+"&branches="+branch

// trigger the build
new URL(triggerUrl).getContent()
}

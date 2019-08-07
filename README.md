# AndroidScreenshotReporting
* Tool used to help QA teams for easier Android bug reporting.
* This library enables you to take screenshots of your applications and directly submit them to a project managament service via email.
* Taken screenshots will capture the entire screen structure (activity + dialogs + etc.), without status bar and native Android buttons.
<br/><br/><br/>

### To implement in your project, do as follows:
* Find the latest version tag at the [link](https://jitpack.io/#bornfight/AndroidScreenshotReporting).
* Add the following dependency to `build.gradle`:<br/>
`implementation 'com.github.bornfight:AndroidScreenshotReporting:VERSION_TAG'`
* Initialize the library in your App class:
```kotlin
   // Add your project managament service email and default subjects here:
   ScreenshotReporting.init(application, "bug.report@project.com", "@lukal")
```
<br/>

### QA general usage:
* While using an app with enabled dependency, just tap the notification card which will be shown to you at the notification panel.
* Next, the screenshot of the app will be taken and placed as the attachment in the email body.
<br/><br/>

### QA Teamwork usage:
* If you're using **Teamwork** as your project managament service, then you can use the following tricks to customize the task you're about to create via email:
  * Pass your Teamwork project email address as init param of ScreenshotReporting library (devs).
  * Tag responsible people with Teamwork tags in the email subject: `@lukal` `@tomislav`
  * Add task tags in the email subject: #android #[three word task]
  * Add task name in the email subject: Task name
  * Add task priority before the task name: !Task name
  * Add task description in the email body.
  * **To sum it up**, for **medium priority** task which should be **assigned to** `@lukal` and `@tomislavs` and **tagged** *Android*, **email subject** would be structured as: @lukal @tomislav #Android !!Your task name text
 
  * For more information about Teamwork email-task submitting tricks, visit the [link](https://support.teamwork.com/projects/tasks/posting-tasks-via-email).

---
services: active-directory
author: sagonzal
platforms: Java
level: 200
client: Java Console Application
service: Microsoft Graph
endpoint: AAD v2.0
---

# Java Console application letting users sign-in with Username/password to call Microsoft Graph API


## About this sample

### Overview

This sample demonstrates how to use MSAL4J to:

- authenticate the user silently using username and password.
- and call to a web API (in this case, the [Microsoft Graph](https://graph.microsoft.com))

![Topology](./ReadmeFiles/Java-Native-Diagram.png)

### Scenario

The application obtains a token through username and password, and then calls the Microsoft Graph to get information about the signed-in user.

Note that Username/Password is needed in some cases (for instance devops scenarios) but it's not recommanded because:

- This requires having credentials in the application, which does not happen with the other flows.
- The credentials should only be used when there is a high degree of trust between the resource owner and the client and when other authorization grant types are not
   available (such as an authorization code).
- Do note that this attempts to authenticate and obtain tokens for users using this flow will often fail with applications registered with Azure AD. Some of the situations and scenarios that will cause the failure are listed below  
  - When the user needs to consent to permissions that this application is requesting. 
  - When a conditional access policy enforcing multi-factor authentication is in force.
  - Azure AD Identity Protection can block authentication attempts if this user account is compromised.
  - The user's pasword is expired and requires a reset.

While this flow seems simpler than the others, applications using these flows often encounter more problems as compared to other flows like authorization code grant. 

The modern authentication protocols (SAML, WS-Fed, OAuth and OpenID), in principal, discourages apps from handling user credentials themselves. The aim is to decouple the authentication method from an app. Azure AD controls the login experience to avoid exposing secrets (like passwords) to a website or an app.

This enables IdPs like Azure AD to provide seamless single sign-on experiences, enable users to authenticate using factors other than passwords (phone, face, biometrics) and Azure AD can block or elevate authentication attempts if it discerns that the user’s account is compromised or the user is trying to access an app from an untrusted location and such.


## How to run this sample

To run this sample, you'll need:

- Working installation of Java and Maven
- An Internet connection
- An Azure Active Directory (Azure AD) tenant. For more information on how to get an Azure AD tenant, see [How to get an Azure AD tenant](https://azure.microsoft.com/en-us/documentation/articles/active-directory-howto-tenant/)
- A user account in your Azure AD tenant. This sample will not work with a Microsoft account (formerly Windows Live account). Therefore, if you signed in to the [Azure portal](https://portal.azure.com) with a Microsoft account and have never created a user account in your directory before, you need to do that now.

## Quick Start

Getting started with the sample is easy. It is configured to run out of the box with minimal setup.

### Step 1: Download Java (8 and above) for your platform

To successfully use this sample, you need a working installation of [Java](http://www.oracle.com/technetwork/java/javase/downloads/index.html) and [Maven](https://maven.apache.org/).

### Step 2:  Clone or download this repository

From your shell or command line:

`git clone https://github.com/Azure-Samples/active-directory-java-native-headless-v2.git `

### Step 3:  Register the sample with your Azure Active Directory tenant

To register the project, you can:

- either follow the steps in the paragraphs below
- or use PowerShell scripts that:
  - **automatically** create for you the Azure AD applications and related objects (passwords, permissions, dependencies)
  - modify the projects' configuration files.

If you want to use this automation, read the instructions in [App Creation Scripts](./AppCreationScripts/AppCreationScripts.md)

### First step: choose the Azure AD tenant where you want to create your applications

As a first step you'll need to:

1. Sign in to the [Azure portal](https://portal.azure.com).
1. On the top bar, click on your account, and then on **Switch Directory**. 
1. Once the *Directory + subscription* pane opens, choose the Active Directory tenant where you wish to register your application, from the *Favorites* or *All Directories* list.
1. Click on **All services** in the left-hand nav, and choose **Azure Active Directory**.

> In the next steps, you might need the tenant name (or directory name) or the tenant ID (or directory ID). These are presented in the **Properties**
of the Azure Active Directory window respectively as *Name* and *Directory ID*

#### Register the app app (Native-Headless-Application)

1. In the  **Azure Active Directory** pane, click on **App registrations** and choose **New application registration**.
1. Enter a friendly name for the application, for example 'Native-Headless-Application' and select 'Native' as the *Application Type*.
1. For the *Redirect URI*, enter `https://<your_tenant_name>/Native-Headless-Application`, replacing `<your_tenant_name>` with the name of your Azure AD tenant.
1. Click **Create** to create the application.
1. In the succeeding page, Find the *Application ID* value and copy it to the clipboard. You'll need it to configure the configuration file for this project.
1. Then click on **Settings**, and choose **Properties**.
1. Configure Permissions for your application. To that extent, in the Settings menu, choose the 'Required permissions' section and then,
   click on **Add**, then **Select an API**, and type `Microsoft Graph` in the textbox. Then, click on  **Select Permissions** and select **User.Read**.
1. Navigate back to the 'Required permissions' section, and click on **Grant Permissions**

### Step 4:  Configure the sample to use your Azure AD tenant

In the steps below, ClientID is the same as Application ID or AppId.

#### Configure the app project

1. Open the `src\main\java\PublicClient.java` file
1. Find the line `private final static String CLIENT_ID` and replace the existing value with the application ID (clientId) of the `Native-Headless-Application` application copied from the Azure portal.

### Step 5: Run the sample

From your shell or command line:

- `$ mvn package`

This will generate a `public-client-adal4j-sample-jar-with-dependencies.jar` file in your /targets directory. Run this using your Java executable like below:

- `$ java -jar public-client-adal4j-sample-jar-with-dependencies.jar`

### You're done!

Your command line interface should prompt you for the username and password and then access the Microsoft Graph API to retrieve your user information.
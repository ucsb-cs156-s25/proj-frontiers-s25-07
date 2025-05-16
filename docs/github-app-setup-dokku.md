Next, we'll set up the Github App. To do so, go to https://github.com/

Then, click your profile icon. Click "Settings". Then, Click "Developer Settings", on the bottom of the toolbar on the left.

Select "New Github App". Fill in an appropriate name, and write it down. You will need it later.
![image](https://github.com/user-attachments/assets/3d0fe501-318c-4907-a267-eff44f06f17a)


For the homepage url, fill in `https://<appname>.dokku-<dokku-number>.cs.ucsb.edu`.

![image](https://github.com/user-attachments/assets/c0e06e2a-2aad-4b3a-af55-46448ed571ee)



For Callback URLs, select "Add Callback URL"


In the first callback URL, fill in `https://<appname>.dokku-<dokku-number>.cs.ucsb.edu/api/courses/link`. For the second URL, fill in `https://<appname>.dokku-<dokku-number>.cs.ucsb.edu/login/oauth2/code/github`, replacing appname with the name of your app, and dokku-number with your dokku installation.



Click the checkbox for "Request user authorization (OAuth) during installation"

![image](https://github.com/user-attachments/assets/bd58b939-6c3f-481a-a7f5-a118f65acc1b)


Scroll further and under webhooks, fill in the following url where *appname* is your appname and *xx* is your dokku server:
```
https://appname.dokku-xx.cs.ucsb.edu/api/webhooks/github
```

Scroll down to permissions, and under repository, set the following accesses:
- Administration: Read and Write
- Contents: Read and Write
- Metadata: Read-only
- Workflows: Read and Write

Under Organization, select the following permissions:
- Administration: Read and Write
- Members: Read and Write


![image](https://github.com/user-attachments/assets/5ba94bdb-d4ce-4911-a80f-248e8e231a24)

Then, scroll further and under "Subscribe to Events" select "Organization"

![image](https://github.com/user-attachments/assets/65491ad0-ef2b-4542-891b-852365f2366b)


Then, scroll further and under "Where can this Github App be installed?" select "Any Account"

Click "Create".

Now, Select "Generate Client Secret". Copy this client secret to a safe location, you will use it in a few minutes. Copy the Client ID as well.

![image](https://github.com/user-attachments/assets/856cf882-b6f3-44a5-b70b-115531bb8cae)


Scroll down to "Private Keys" and select "Generate a Private Key"

![image](https://github.com/user-attachments/assets/7c2b958a-f912-4972-af63-9ff2c30339cd)


Though we will now start using other applications, keep this Github window open. You'll need it later.

This will download a private key to your computer.

Next, we will switch the standard of the key we just downloaded so Java can understand it.
To do so, open a terminal in the folder the key is in, and run the following command, replacing `<file-name>` with the name of the key.
```bash
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in <file-name> -out pkcs8.key
```

Next, we're going to take the output of this newly created file and set it as an environmental variable on Dokku. To do so, output the file into your terminal with the following command:
```bash
cat pkcs8.key
```

Copy the output from this command and connect to your Dokku installation. As this is a multiline variable, it will need to be between a set of quotes. Make sure that you paste it between the quotes, otherwise the variable will only be set to the first line of the key. Use the following command, replacing <appname> with your app name and <file-output> with the copied output from the previous step:
```bash
dokku config:set --no-restart <appname> app_private_key="<file-output>"
```

Then, set your Github Client ID and Github Client Secret with the following commands respectively:
```bash
dokku config:set --no-restart <appname> GITHUB_CLIENT_ID=<client-id>
dokku config:set --no-restart <appname> GITHUB_CLIENT_SECRET=<client-secret>
```

Then, set your Google Cloud Credentials from earlier with the following commands:
```bash
dokku config:set --no-restart <appname> GOOGLE_CLIENT_ID=<client-id>
dokku config:set --no-restart <appname> GOOGLE_CLIENT_SECRET=<client-secret>
```

Next, set up a Postgres database for your app. A separate set of directions for this step are listed [here](https://ucsb-cs156.github.io/topics/dokku/postgres_database.html#postgres-database---how-to-deploy-a-postgres-database).

Next, sync the app with the repository.
```bash
dokku git:sync <appname> https://github.com/ucsb-cs156/proj-frontiers main
```

Next, start your app.
```bash
dokku ps:rebuild <appname>
```

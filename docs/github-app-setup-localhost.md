# Installing on Localhost
First, clone the repository. This can be done with the following command:
```bash
git clone git@github.com:ucsb-cs156/proj-frontiers.git
```
Then, obtain a set of Google Cloud Credentials. Directions for obtaining these can be found [here](https://ucsb-cs156.github.io/topics/oauth/oauth_google_setup.html).
Next, we'll set up the Github App. To do so, go to https://github.com/

Then, click your profile icon. Click "Settings". Then, Click "Developer Settings", on the bottom of the toolbar on the left.

Select "New Github App". Fill in an appropriate name, and write it down. You will need it later.
![image](https://github.com/user-attachments/assets/3d0fe501-318c-4907-a267-eff44f06f17a)


For the homepage url, fill in `http://localhost:8080`.

![image](https://github.com/user-attachments/assets/bec66087-ca4a-4fc4-af3d-9ad663c24eb2)


For Callback URLs, select "Add Callback URL"


In the first callback URL, fill in `http://localhost:8080/api/courses/link`. For the second URL, fill in `http://localhost:8080/login/oauth2/code/github`.

Click the checkbox for "Request user authorization (OAuth) during installation"

![image](https://github.com/user-attachments/assets/a06af72c-e08d-4f47-bd6a-91b5d1aef65f)


Scroll down to permissions, and under repository, set the following accesses:
- Administration: Read and Write
- Contents: Read and Write
- Metadata: Read-only
- Workflows: Read and Write

Under Organization, select the following permissions:
- Administration: Read and Write

Then, scroll further and uncheck "Active" under "Webhooks"

![image](https://github.com/user-attachments/assets/74119317-b1a5-40c8-88ce-d7e394f7e5a6)


Then, scroll further and under "Where can this Github App be installed?" select "Any Account"

Click "Create".

Now, Select "Generate Client Secret". Copy this client secret to a safe location, you will use it in a few minutes. Copy the Client ID as well.

![image](https://github.com/user-attachments/assets/856cf882-b6f3-44a5-b70b-115531bb8cae)


Scroll down to "Private Keys" and select "Generate a Private Key"

![image](https://github.com/user-attachments/assets/7c2b958a-f912-4972-af63-9ff2c30339cd)


Though we will now start using other applications, keep this Github window open. You'll need it later.

This will download a private key to your computer. Move this file into the repository directory. Importantly, **Do not commit this file**.

Next, we will switch the standard of the key we just downloaded so Java can understand it.
To do so, open a terminal in the repository folder, and run the following command, replacing `<file-name>` with the name of the key.
```bash
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in <file-name> -out pkcs8.key
```

Next, create a copy of `secrets.yaml.EXAMPLE` with the following command:
```bash
cp secrets.yaml.EXAMPLE secrets.yaml
```

Now, we're going to take the key we generated and place it into secrets.yaml. Output the key into the terminal with the following command:
```bash
cat pkcs8.key
```

Copy the output from this command, and paste it into secrets.yaml, replacing add your key here with the output. The file should now look like this:
```yaml
app:
  private:
    key: "-----BEGIN PRIVATE KEY-----
<Your Key>
-----END PRIVATE KEY-----
"
```

Next, we'll fill in `.env`. Create a copy of it from `.env.SAMPLE` with:
```bash
cp .env.SAMPLE .env
```

Fill in your Google secrets, from the article at the beginning. Additionally, fill in your Github Client ID and Client Secret, named `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GITHUB_CLIENT_ID`, and `GITHUB_CLIENT_SECRET` respectively.
Additionally, make sure your email is on the list of admin emails.

Next, start your project:
```bash
mvn spring-boot:run
```

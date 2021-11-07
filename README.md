# Book-Viewer
An application for uploading books in PDF format and viewing their pages


## Prerequisite steps
The server is designed to upload PDF and converted image files to Google Cloud Storage (GCS)

It is required to create two buckets in the GCS project that will be used
- login to GCS and create/open the project
- create two buckets inside the project, e.g.:
  - book_pdf
  - book_pages
- update application properties with bucket names
  - **com.vranisimo.bookviewer.gcs.bucket.book**=book_pdf
  - **com.vranisimo.bookviewer.gcs.bucket.bookpage**=book_pages


In order to enable connection between book_viewer server and GCS, we need to generate GCS credentials JSON file.

More information can be found here https://cloud.google.com/docs/authentication/getting-started#creating_a_service_account

After the JSON is generated, it should be renamed as "gcs_credentials.json" and copied to project's root folder

The last step is to copy the GCS project id and set it to spring.cloud.gcp.project-id property inside the application.properties file.


## Building the project

To build the project, clone the git repository to local machine and execute the command in the project root folder:

    gradlew build -x test

## Runing the app

After the build is finished, a docker compose script should be executed

This can be easily achieved by running the script from the project root folder:

    docker-compose up --build

Docker will download required docker images for Postgres and Kafka and automatically start four docker containers:

    - Spring Boot server (book_viewer)
    - Postgres server
    - Zookeeper server
    - Kafka server


As configured in docker-compose.yml file, all containers will be located in the same virtual network with specific IP addreses assigned. This will allow Spring boot server to communicate with both Kafka and Postgres servers.

All servers should be automatically started in about a minute, depending on the host machine performance that is running docker.

Since that main Spring docker container "book-viewer" initially installs the ImageMagick program used for PDF processing, please wait for installation to complete during the first run.

Once the installation is completed, REST API server should be available at port 8080

------------------------------------------------------------------------------

# REST API

REST API provides all the actions required for use an application.
There are two main endpoints, **book** and **user**.

User endpoint allows both user registration and login and Book endpoint is for all book-related actions.

The REST API is available at http://localhost:8080



# User endpoint

The simple user registration can be achieved on this endpoint

Once the user is registered, it can login to application

------------------------------------------------------------------------------

## Register
`GET /user/register`

### Example
`GET http://localhost:8080/register`

### Body
    {
        "username": "username",
        "email": "user@test.com",
        "password": "password"
    }


### Error example
#### **REQUEST**
`GET http://localhost:8080/user/register`

    {
        "username" : "username",
        "email" : "user@test.com",
        "password" : "pass"
    }

#### **RESPONSE**
`400 Bad Request`

    {
        "errorMessage": "Password length must be between 8 and 64 characters."
    }

------------------------------------------------------------------------------
## Login

`POST /user/login`

To retrieve JWT access token used for other actions, the user must first login.
JWT token will expire in 24 hours, or after the period specified in application properties.

### Example

#### **REQUEST**
`POST http://localhost:8080/login`

    {
        "username" : "username",
        "password" : "password"
    }
#### **RESPONSE**
`200 OK`

    {
        "access_token": "eyJhbGciOi..."
    }

### Error example
#### **REQUEST**
`POST http://localhost:8080/login`

    {
        "username" : "username",
        "password" : "wrongpassword"
    }

#### **RESPONSE**
`401 Unauthorized`

    {
        "errorMessage": "Incorrect password"
    }


# Book endpoint

Book endpoint is used for all main actions the user can perform in the application

## Get books

`GET /book`
`GET /book?isbn=ISBN`

If called without the isbn parameter, the response will contain all available book from the system, that were previously uploaded by (any) user

### Example without ISBN, will return all books

#### **REQUEST**
`GET http://localhost:8080/book`

    The authorization header must contain access token

#### **RESPONSE**
`200 OK`

    [
        {
            "isbn": "9780080674131",
            "page_count": 3,
            "processed_page_count": 3,
            "is_processed": true
        }
    ]


### Example with ISBN, will return a specific book

#### **REQUEST**
`GET /book/?isbn=9780080674131`

    The authorization header must contain access token

#### **RESPONSE**
`200 OK`

    {
        "isbn": "9780080674131",
        "page_count": 3,
        "processed_page_count": 3,
        "is_processed": true
    }
    
------------------------------------------------------------------------------
## Upload book PDF

`POST /book/upload?isbn=ISBN`

Allows user to the upload PDF file to server.

After the PDF file is successfully uploaded, it will be automatically processed by Kafka's consumers.

Once the internal processing is done, book's is_processed property will change to true and users are allowed to get signed expirable URLs for a specific PDF page


### Example

#### **REQUEST**
`POST http://localhost:8080/book/upload?isbn=9781734314502`

    The authorization header must contain access token
    A PDF file must be attached in the body as a multipart/form-data
    ISBN must be provided

#### **RESPONSE**
`200 OK`

    {
        File is successfully uploaded
    }

------------------------------------------------------------------------------
## Get expirable signed URL of PDF page

`GET /book/getUrl?isbn=ISBN&pageNumber=PAGE_NUMBER`

Allows user to get expirable signed URL of the specific PDF page

The URL navigates to PDF page (jpeg format) that is stored on remote Google Cloud Storage server

### Example

#### **REQUEST**
`GET http://localhost:8080/book/upload?isbn=9781734314502`

    The authorization header must contain access token
    Parameters:
        ISBN- ISBN of the PDF
        pageNumber- a number of PDF page, starting from 1

#### **RESPONSE**
`200 OK`

    https://storage.googleapis.com/book_pages/9782775812125_1.jpeg...

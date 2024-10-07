# Key-Value-File-Store


## Tasks

- Create, read, and delete key-value pairs
- Supports TTL for key expiration
- Automatic cleanup of expired keys
- Persistent storage in JSON format
- Thread-safe operations
- Configurable file size limit

## Table of Contents

- [Installation](#installation)
- [Cloning the Repository](#cloning-the-repository)
- [Setup](#setup)
- [Usage](#usage)

## Installation

### Prerequisites

- **Java Development Kit (JDK) 11 or higher**: Ensure you have JDK installed on your system. You can download it from [Oracle's website](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html) or use OpenJDK.
- Download any IDE which is suitable for you eg:(Eclipse,STS)
- Download json.org file from [This website](https://mvnrepository.com/artifact/org.json/json/20140107)

## Easy way
- Open your IDE
```bash
File > New Java Project 
```
- Name your project and hit enter
- Right click on the project
```bash
build path >> Configure Build Path >> Libraries >> Add External JARs 
```
and add downloaded json-<verison>.jar
- Create a class and copy and paste the KeyValueDataStore class from the repository
```bash
src > KeyValueDataStore.class
```
and run the project as Java Application.

### Cloning the Repository

-  Open a terminal or command prompt from your IDE.
-  Clone the repository using the following command:
```bash
git clone https://github.com/Skmnk/Key-Value-File-Store.git
```
-  Navigate to the project directory:
```bash
cd Key-Value-File-Store
```
## Setup
- Right click on the project and 
```bash
build path >> Configure Build Path >> Libraries >> Add External JARs 
```
- and add the downloaded json-<version>.jar file.

## Usage
-  Right click on the project 
```bash
Run As>> Java Application
```

Now you can perform all the objectives given.

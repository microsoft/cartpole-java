# Build Docker Image

You can use the build-docker.bat file (Windows) to build the docker image.

```
build-docker.bat <ACR_NAME> <DOCKER_IMAGE_NAME> [<VERSION>]
```

where

- <ACR_NAME> is container registry name
- <DOCKER_IMAGE_NAME> is the name of your docker image
- <VERSION> (optional) is the version name. Will default to **latest** if not specified

# Run Docker Image

To run the docker image after building it, use the command

```
docker run --rm -it -e SIM_ACCESS_KEY="<ACCESS_KEY>" -e SIM_API_HOST="https://api.bons.ai" -e SIM_WORKSPACE="<WORKSPACE_ID>" <DOCKER_IMAGE_NAME>
```

where

- <WORKSPACE_ID> is your workspace ID
- <ACCESS_KEY> is your access key
- <DOCKER_IMAGE_NAME> is the name of your docker image
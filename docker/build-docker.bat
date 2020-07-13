@echo off
if [%1]==[] goto usage
if [%2]==[] goto usage


SET image_name=%1
SET container_registry=%2

ECHO Building image %image_name%

copy ..\target\microsoft.bonsai.samples.cartpole-1.0-jar-with-dependencies.jar lib

ECHO Building image %image_name%

docker build -t %image_name%  .

ECHO Logging in to %container_registry%

cmd /c az acr login --name %container_registry%

ECHO Tagging %image_name% and pushing to %container_registry%.azurecr.io/%image_name%:latest

docker tag %image_name% %container_registry%.azurecr.io/%image_name%:latest
docker push %container_registry%.azurecr.io/%image_name%:latest

ECHO Complete

:usage
@echo Usage: %0 ^<docker_image_name^> ^<container_registry_name^>
exit /B 1
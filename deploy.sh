sbt universal:packageZipTarball && docker build -t aleksys/antlrpad . && docker push aleksys/antlrpad && ansible-playbook -i deploy/hosts deploy/deploy.yaml

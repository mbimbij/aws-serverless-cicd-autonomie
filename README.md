# Serverless CI/CD with safedeployments, as code.

:fr: Sommaire / :gb: Table of Contents
=================

<!--ts-->

- [:fr: Description du projet](#fr-description-du-projet)
- [:gb: Project Description](#gb-project-description)


<small><i><a href='http://ecotrust-canada.github.io/markdown-toc/'>Table of contents generated with markdown-toc</a></i></small>


---

# :fr: Description du projet

## Présentation

### Introduction générale

Le but de ce projet est d'implémenter une pipeline CI/CD pour une application Serverless.

L'application est une simple fonction lambda, renvoyant "hello from my-app-autonome - vXX", où l'on fait varier "XX" 
pour vérifier que la pipeline est en mesure déployer une nouvelle version à jour.

### Présentation de la pipeline

Puisqu'il s'agit de ma première tentative d'implémentation, la pipeline est très simple: 

![](docs/1-pipeline-draw.png)

- "Source" récupère le projet depuis Github
- "Build" lance `sam package` et place de template résultant dans S3
- "Deploy" déploie la fonction lambda (en mode "canary"), à partir de la sortie de l'étape "Build"

Le code de la pipeline se trouve dans un template cloudformation: `infra-stack/infra-stack.yml`. 
Voici les ressources déployées par le template:

![](docs/2-infra-stack-designer-view.png)

on a donc: 

- un projet `CodePipeline` pour l'orchestration de la pipeline, avec un rôle associé 
- un bucket `S3` qui va servir à transmettre la sortie d'une étape en entrée de l'étape suivante
- pour l'étape "Source": 
  - une connection `Github`
- pour l'étape "Build":
  - un projet `CodeBuild` et un rôle `IAM` associé, permettant de:
    - écrire des logs dans `CloudWatch`
    - pousser le résultat du build dans `S3`
- pour l'étape "Deploy":
  - un stage de `CodePipeline`, qui va récupérer le template SAM processé par  `CodeBuild`, et configuré pour créer / mettre un jour une stack `CloudFormation` associée à ce template SAM. 
  - un rôle `IAM` que `CodePipeline` va passer à la stack `CloudFormation`/`SAM`. C'est cette stack qui va déployer la fonction `Lambda`   

## Organisation du project

- Le répertoire `infra-stack` contient le template `CloudFormation` de la pipeline
- Le fichier `sam-template.yml` contient ... le template SAM
- Le module maven `main-function` contient 
  - le code java de la fonction `Lambda`
  - un test d'intégration, invoquant la lambda avant de procéder au déploiement (déplacement progressif d'un alias)
- Le module maven `integration-tests` contient du code permettant de wrapper l'éxécution du test d'intégration dans un fonction `Lambda`, car `SAM` n'accepte que des fonctions `Lambda` en pre et post traffic hook. 
  - oui c'est de l'archi héxa appliqué à l'éxécution de tests :) 

##  Lancement et éxécution du projet

### Pré-requis

Installez la CLI AWS et éxécutez "aws configure".
[https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html](https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html) 
et sélectionnez la version de la documentation adaptée à votre cas.

### Création de la pipeline

`aws cloudformation create-stack --stack-name hello-app-pipeline-stack --template-body file://infra-stack/infra-stack.yml --parameters ParameterKey=ApplicationName,ParameterValue=hello-app --capabilities CAPABILITY_NAMED_IAM`

# :gb: Project Description
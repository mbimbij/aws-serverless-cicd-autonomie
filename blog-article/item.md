
---
title: Implémentation d'une pipeline CI/CD pour des application AWS Serverless - partie1
date: 12:30 03/23/2021
author: Joseph M'Bimbi-Bene
hero_classes: 'text-light overlay-dark-gradient'
hero_image: article-logo.png
taxonomy:
    category: blog
    tag: [devops, cloud, serverless]
---

Dans cette article, nous allons implémenter et faire évoluer une pipeline de CI/CD reproduisant les exemples et architectures de référence fournis par AWS.

===

Cet article est le premier d'une série de 6 à 10 articles.

Nous allons implémenter le début d'une pipeline de CI/CD, uniquement les étapes "Source" et "Build" bouchonnée, sans code applicatif.

Nous créerons la pipeline d'abord manuellement, via la console web. 
Ensuite nous automatiserons ce processus par un template CloudFormation.

Au prochain article, nous implémenterons le code de la fonction `Lambda` en java, et nous intégrerons le code applicatif à la pipeline.

### Sommaire

- [Introduction, Description du projet](#introduction-description-du-projet)
  * [Cible](#cible)
  * [Etapes](#etapes)
  * [Plan de l'étape #1](#plan-etape-1)
  * [Plan de l'article](#plan-article)
- [Implémentation Manuelle](#implmentation-manuelle)
- [Références](#références)

<small><i><a href='http://ecotrust-canada.github.io/markdown-toc/'>Table of contents generated with markdown-toc</a></i></small>


###  <a name="introduction-description-du-projet"></a> Introduction, Description du projet

Le but de cet série d'article est d'implémenter et de faire évoluer une pipeline de CI/CD reproduisant des exemples et 
architectures de référence fournis par AWS.

Ainsi, ce que l'on produira devrait être proches de "best practices", et l'on sera confiant dans ce que l'on a produit.

#### <a name="cible"></a> Cible

Au bout de cette série d'article, le résultat final produit devrait ressembler à :

![](1-pipeline-vue-logique-1.png)

ici une vue des composants AWS correspondants:

![](2-pipeline-vue-implementation.png)

cf [https://github.com/awslabs/aws-refarch-cross-account-pipeline](https://github.com/awslabs/aws-refarch-cross-account-pipeline)

#### <a name="etapes"></a> Etapes

Pour arriver à ce résultat final, nous allons procéder par étapes :

![](3-etapes-implementation.png)

- Une pipeline "simple", qui build et déploie la fonction lambda dans le compte courant, correspondant à [l'implémentation d'exemple AWS suivante](https://github.com/aws-samples/aws-lambda-sample-applications/tree/master/CICD-toolchain-for-serverless-applications)
  -  On rajoutera des tests d'intégration en "pre-traffic hook", c'est à dire se déclanchant après le déploiement mais avant la redirection du traffic sur la nouvelle version
- Rajout d'un étape de "test" à la pipeline précédente, déployant l'application dans un autre compte AWS, dédié au test
- Version finale de la pipeline, correspondant à [l'architecture de référence pour les déploiement cross-account](https://github.com/awslabs/aws-refarch-cross-account-pipeline)

#### <a name="plan-etape-1"></a> Plan de l'étape #1

Nous procèderons de manière incrémentale pour implémenter chaque étape.
Ainsi nous subdiviserons la réalisation de l'étape #1 dans les sous-étapes suivantes:

- 1.1 - Implémentation des étapes "Source" + "Build" bouchonné
- 1.2 - Implémentation de l'application `Lambda` en Java
- 1.3 - Intégration du code application et de la pipeline
- 1.4 - (Bonus) Implémentation de tests automatisés pour la création de la pipeline

Pour éviter le "Big Design Upfront", source de stagnation, de frustration, et de toute manière très vite périmé en pratique, nous verrons le moment venu le détail des subdivisions entre les étapes #1 et #2, et entre les étapes #2 et #3.

Dans un premier temps, avoir une idée claire des grandes étapes est suffisant.

Le plan de l'implémentation de cette étape #1 pouvant lui-même évoluer au cours de l'implémentation, restons agiles et pragmatiques.


#### <a name="plan-article"></a> Plan de l'article

Dans cet article, nous allons réaliser l'étape 1.1 décrite dans la section précédente: "1.1 - Implémentation des étapes "Source" + "Build" bouchonné"

Nous allons dans un premier temps implémenter ce début de pipeline manuellement, ensuite nous allon l'automatiser via ̀`CloudFormation`: 

### <a name="implementation-manuelle"></a> Implémentation Manuelle

Nous allons procéder de la manière suivante pour l'implémentation manuelle

1. création d'un bucket S3
2. création de roles IAM 
  + 2.1 pour codebuild
  + 2.2 pour codepipeline
3. création d'une pipeline CodePipeline
  + 3.1 source: création d'une connexion github
  + 3.2 build: création d'un projet codebuild
4. lancement de la pipeline
5. inspection du bucket S3 et vérification de la présence des sources

======

1. création d'un bucket S3 
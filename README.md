## Распознавание атрибутов лиц


Приложение поддерживает распознавание возраста, пола, этнической принадлежность, аттрибутов из Celeba: очки, улыбка, ... (список аттрибутов см. в app/src/main/assets/celeba_attributes.txt).

Можно сгруппировать изображения по полу, нажав в меню  "Group by gender". 

Сеть для определения аттрибутов из Celeba была взята из https://github.com/kartikbatra056/CelebFaces-Attributes-Prediction/tree/875380a038b5af12e4caf1897726fea5a9f6a917 и сконвертирована в pytorch light.


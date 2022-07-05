Mise en place d'une API de recherche REST utilisation les "Spécifications" pour effectuer le filtrage, paging et sorting des résultats.



Voici la structure d'une requête:

https://url-application/api/entite?page=0&size=10&sort=nomPropriete&nomPropriete|operateur=valeur&nomPropriete2=valeur2&nomPropriete3.sousPropriete1|operateur=valeur&nomPropriete4.sousPropriete1,sousPropriete2|containsIn=valeur

Si aucun opérateur n'est spécifié, une recherche de type "equals" sera faite par défaut. Il faut donc soit mettre le "|" et l'opérateur voulu soit ne rien mettre.

Il est possible pour les propriétés de type "Objet" de faire la recherche sur une de ses propriétés en mettant un point "propriete.sousPropriete".

Il est également possible pour des sous propriétés d'objets de faire des recherches de type "contains" sur la concatenation de ceux-ci => "propriete.sousPropriete,sousPropriete2|containsIn=val1,val2"

Il est également possible de faire la recherche ci-dessous sur differentes proprietes en faisant un "OR" grâce à l'opérateur "/". Ex => "propriete.sousPropriete,sousPropriete2/propriete2.sousPropriete,sousPropriete2|containsIn=val1,val2".



Voici un exemple réel de requête:

http://localhost:8080/api/produit?page=0&size=10&sort=nomPropriete&numBP|startsWith=art&code=test



voici la liste des opérateurs:

equals	Aucun restriction
field1|equals=true

field1|equals=blabla

field1|equals=2

notEquals	Aucun restriction
field1|notEquals=true

field1|notEquals=blabla

field1|notEquals=2

specified	Boolean => true | false
field1|specified=true          (on test si la propriété est bien renseignée)

field1|specified=false         (on test la nullité d'une propriété)

contains	String => blablabla
notContains	String => blablabla
containsIn	String[] => blabla,blabla2
startsWith	String => blablabla
endsWith	String => blablabla
greaterThan	Numeric => 2 | 2,2
greaterThranOrEqual	Numeric => 2 | 2,2
lessThan	Numeric => 2 | 2,2
lessThanOrEqual	Numeric => 2 | 2,2
in	String[] | Numeric[] | Date[]
notIn	String[] | Numeric[] | Date[]	



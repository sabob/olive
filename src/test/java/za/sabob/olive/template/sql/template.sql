SELECT * FROM Company WHERE

--{{#country}}
country = :country
--{{/country}}

--{{#code}}
AND 
code = :code
--{{/code}}

{{#batch}}
insert into blah where x = :x
{{/batch}}

Code: {{code}}

{{#hasWhere}}
YAY {{hasWhere}}
{{/hasWhere}}

{{^hasWhere}}
NOO
{{/hasWhere}}
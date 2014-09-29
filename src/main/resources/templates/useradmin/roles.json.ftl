<#ftl encoding="utf-8">
{ "propsAndRoles" : [
<#list roller as rolle>
<#include "role.json.ftl"/><#if rolle_has_next>,</#if>
</#list>
]}
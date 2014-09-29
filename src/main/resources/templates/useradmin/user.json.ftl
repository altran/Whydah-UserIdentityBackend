<#ftl encoding="utf-8">
{
<#assign identity=user.identity>
"identity": <#include "useridentity.json.ftl"/>,
"propsAndRoles":[
<#list user.propsAndRoles as rolle>
   <#include "role.json.ftl"/><#if rolle_has_next>,</#if>
</#list>
]
}
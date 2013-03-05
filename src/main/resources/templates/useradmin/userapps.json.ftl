{"applications" : [
<#list allApps as app>
    {
        "appId" : "${app.appId?js_string}",
        "applicationName" : "${app.name?js_string}",
        "hasRoles" : ${myApps?seq_contains(app.appId)?string("true", "false")}
    }<#if app_has_next>,</#if>
</#list>
]}
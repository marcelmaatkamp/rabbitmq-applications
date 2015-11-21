FROM rabbitmq:management

RUN rabbitmq-plugins enable --offline \
    rabbitmq_federation \
    rabbitmq_federation_management \
    rabbitmq_shovel \
    rabbitmq_shovel_management \
    rabbitmq_mqtt \
    rabbitmq_auth_backend_ldap \
    rabbitmq_management

RUN echo "[ \
  {rabbit, [{auth_backends, [rabbit_auth_backend_internal,  
rabbit_auth_backend_ldap]}]  },
  {rabbit_auth_backend_ldap,
   [ {servers,               ["ldap.zozee.com"]},
     {user_dn_pattern,       "uid=${username},ou=people,dc=zozee,dc=com"},
     {vhost_access_query,    
{exists,"ou=${vhost},ou=people,dc=zozee,dc=com"}},
     {resource_access_query,
       {for, [{resource, exchange,{for, [{permission, configure,
                                          { in_group, 
"cn=zozee,ou=groups,dc=zozee,dc=com" }
                                         },
                                         {permission, write, {constant, 
true}},
                                         {permission, read,  {constant, 
true}}
                                        ]}},
              {resource, queue, {constant, true}} ]}},
       {tag_queries,           [{administrator, {constant, false}}]},
       {use_ssl,               false},
       {port,                  389},
       {log,                   true} ] }
]." > /etc/rabbitmq/rabbitmq.config"
EXPOSE 1883 5672 15672 25672

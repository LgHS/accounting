#!/bin/bash

set -ue
set -o pipefail

DIR=$(cd $(dirname $0) && pwd)

INFO_COLOR='\033[0;33m'
ERROR_COLOR='\033[0;3om'
NC='\033[0m' # No Color

info(){
  echo -e ${INFO_COLOR}$@${NC}
}

error(){
  >&2 echo -e ${ERROR_COLOR}$@${NC}
}


function  dcacc(){
	docker-compose -p lghs-accounting -f $DIR//docker-compose.yml $@
}

function waitDbReady(){
	(dcacc  exec -T db psql -U postgres < <( echo 'select 1 ;') 2>&1 > /dev/null  ) || (
		if [ $1 -lt 20 ]
		then
			echo '.'
			sleep 2
			waitDbReady $(( $1 + 1 ))
		else
			error "time out waiting for db"
			exit 1
		fi
	)
}

function getKeycloakToken(){
	curl --data "username=admin&password=pwd&grant_type=password&client_id=admin-cli" http://localhost:8080/auth/realms/master/protocol/openid-connect/token \
	       	| sed 's/.*access_token":"//g' | sed 's/".*//g' \
		|| (
		
		if [ $1 -lt 20 ]
		then
			(>&2 wait for keycloak to be ready)
			sleep 2
			getKeycloakToken $(( $1 + 1 ))
		else
			error "time out waiting for db"
			exit 2
		fi
	)
}

info start db and keycloak
dcacc up -d db keycloak

info wait for db to start
waitDbReady 0

info init db 
cat $DIR/../prepare_db.sql | dcacc exec -T db psql -Upostgres


info generate jook tables
dcacc run --rm app gradle --stacktrace jooq

info fill db with mock data 
cat $DIR/dbMockData.sql | dcacc exec -T db psql -U postgres lghs_accounting 


info get keycloak token

TOKEN=$(getKeycloakToken 1)
info token : $TOKEN

curl -v http://localhost:8080/auth/admin/realms/master/clients -H "Content-Type: application/json" -H "Authorization: bearer $TOKEN"   --data @$DIR/keycloak/accounting.json
curl -v http://localhost:8080/auth/admin/realms/master/users -H "Content-Type: application/json" -H "Authorization: bearer $TOKEN"   --data @$DIR/keycloak/fooBarUser.json

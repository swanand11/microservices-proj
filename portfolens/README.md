# PortfoLens demo runbook

Start the full app with Docker Compose, or build and run it in Minikube. Both options start the frontend, API gateway, market-data service, Analytics, Workflow Coordinator, and the services they need.

## Before the demo

You need Docker with Compose v2 and a Supabase PostgreSQL database. The app stores its account and portfolio data there; the database is not started by these instructions. Use a development database and keep its credentials private.

From this directory, create the local Compose settings without overwriting an existing file:

```sh
test -f .env.compose || cp .env.compose.example .env.compose
```

Edit `.env.compose` and set `DATABASE_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `MARKET_DATA_SERVICE_KEY`, and `ANALYTICS_SERVICE_KEY`. Use the Supabase JDBC URL with `sslmode=require`. Generate a JWT key and separate internal service keys with:

```sh
openssl rand -base64 32
```

The example uses frontend port `8081` and gateway port `8000`. Compose starts RabbitMQ for the application services.

## Option A: Docker Compose

Start all containers in the background:

```sh
docker compose --env-file .env.compose up -d --build
```

Wait for startup and inspect status:

```sh
docker compose --env-file .env.compose ps
docker compose --env-file .env.compose logs --tail=100
```

Open **http://localhost:8081**. Register an account, sign in, create a portfolio, explore market history, record a buy or sell, then review portfolio analytics. Browser API calls use the frontend’s same-origin proxy; the browser does not need Kubernetes or container DNS names.

Check the frontend, gateway, and coordinator:

```sh
curl -fsS http://localhost:8081/healthz
curl -fsS http://localhost:8000/actuator/health/readiness
curl -fsS http://localhost:8003/actuator/health/readiness
```

If a service fails to start, inspect its logs, for example `docker compose --env-file .env.compose logs --tail=200 workflow-coordinator`.

Stop the app and keep its data volumes:

```sh
docker compose --env-file .env.compose down
```

## Option B: Kubernetes with Minikube

You need Docker, `kubectl`, and Minikube. The Kubernetes manifests use locally built images and require a PostgreSQL database plus a RabbitMQ broker reachable from inside the cluster. Compose’s `rabbitmq` hostname is only valid inside the Compose network.

Create the cluster and load the app images:

```sh
./deploy/scripts/setup-minikube.sh
```

Prepare the Kubernetes Secret without replacing an existing one:

```sh
test -f k8s/secrets.yaml || cp k8s/secrets.example.yaml k8s/secrets.yaml
```

Edit `k8s/secrets.yaml` with your PostgreSQL connection and password, JWT and service keys, and cluster-reachable RabbitMQ host and credentials. Then apply it and deploy the manifests:

```sh
kubectl apply -f k8s/secrets.yaml
./deploy/scripts/deploy-k8s.sh
```

Wait for the frontend and gateway, then inspect all pods and service endpoints:

```sh
kubectl rollout status deployment/frontend -n portfolens --timeout=180s
kubectl rollout status deployment/api-gateway -n portfolens --timeout=180s
kubectl get pods,svc,endpoints -n portfolens
```

Forward the frontend Service to an unused local port:

```sh
kubectl port-forward -n portfolens service/frontend 18080:8080
```

Keep that terminal open and browse to **http://127.0.0.1:18080**. Use the same demo flow as Compose. The frontend proxies API requests to the internal `api-gateway` and `workflow-coordinator` Kubernetes Services. Only the frontend needs to be opened in the browser.

In a second terminal, check frontend access and the gateway through the frontend proxy:

```sh
curl -fsS http://127.0.0.1:18080/healthz
curl -i -X POST http://127.0.0.1:18080/api/auth/login \
  -H 'Content-Type: application/json' \
  --data '{"email":"nobody@example.invalid","password":"invalid-password"}'
```

The login check should return HTTP 401 for the deliberately invalid account; that confirms the request reached the API. The direct gateway Service is cluster-internal. If the frontend port-forward cannot bind, choose another unused local port, such as `18081:8080`, and update the URL accordingly.

## If something is not ready

For Compose, inspect the affected container with `docker compose --env-file .env.compose logs --tail=200 <service>`.

For Kubernetes, inspect pod status, events, and logs:

```sh
kubectl get pods -n portfolens
kubectl get endpoints -n portfolens
kubectl get events -n portfolens --sort-by=.lastTimestamp
kubectl logs deployment/workflow-coordinator -n portfolens --all-pods
```

An empty Service endpoint list means no ready pod currently matches that Service. Check the pod readiness and logs, then verify the database and (for Kubernetes) RabbitMQ settings.

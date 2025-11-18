# Deployment Guide

## Kubernetes Deployment

This application is designed to run in Kubernetes with PostgreSQL as the database.

## Prerequisites

- Kubernetes cluster (1.25+)
- kubectl configured
- Container registry access (GitHub Container Registry)
- CloudNativePG operator (for PostgreSQL)

## Architecture

```
┌─────────────────┐
│     Ingress     │
│   (Optional)    │
└────────┬────────┘
         │
    ┌────▼────┐
    │ Service │
    │ backend │
    └────┬────┘
         │
    ┌────▼──────────┐
    │  Deployment   │
    │   backend     │
    │ (Quarkus App) │
    └───────┬───────┘
            │
    ┌───────▼──────┐
    │  PostgreSQL  │
    │   Cluster    │
    │ (CloudNativePG)│
    └──────────────┘
```

## Namespace Setup

Create the namespace:

```bash
kubectl create namespace quarkus-crud
```

## Database Setup

### Install CloudNativePG Operator

```bash
kubectl apply -f https://raw.githubusercontent.com/cloudnative-pg/cloudnative-pg/release-1.21/releases/cnpg-1.21.0.yaml
```

### Create PostgreSQL Cluster

The cluster is managed by CloudNativePG. Create it using the provided manifests:

```bash
kubectl apply -f manifests/database.yaml
```

This creates:
- PostgreSQL cluster with high availability
- Database `quarkus_crud`
- Automatic backups and failover

### Create Database Secret

```bash
kubectl create secret generic database-secret \
  --namespace=quarkus-crud \
  --from-literal=username=postgres \
  --from-literal=password=<your-secure-password>
```

## Application Deployment

### Build Container Image

#### JVM Container

```bash
./gradlew build jib \
  -Djib.to.image=ghcr.io/yuki-js/quarkus-crud:latest \
  -Djib.to.auth.username=<github-username> \
  -Djib.to.auth.password=<github-token>
```

#### Native Container

```bash
./gradlew build -Dquarkus.package.type=native jib \
  -Djib.to.image=ghcr.io/yuki-js/quarkus-crud:latest-native \
  -Djib.to.auth.username=<github-username> \
  -Djib.to.auth.password=<github-token> \
  -PnativeBuild=true
```

### Configure Image Pull Secret

```bash
kubectl create secret docker-registry github-registry-secret \
  --namespace=quarkus-crud \
  --docker-server=ghcr.io \
  --docker-username=<github-username> \
  --docker-password=<github-token>
```

### Deploy Application

```bash
kubectl apply -f manifests/backend.yaml
```

This creates:
- Deployment with 1 replica
- Service exposing port 80
- Health checks configured
- Resource limits set

### Verify Deployment

Check pod status:
```bash
kubectl get pods -n quarkus-crud
```

Check logs:
```bash
kubectl logs -f deployment/backend -n quarkus-crud
```

## Configuration

### ConfigMap

Create application configuration:

```bash
kubectl create configmap backend-config \
  --namespace=quarkus-crud \
  --from-file=application.properties=manifests/application.properties
```

### Secrets

Store sensitive configuration:

```bash
kubectl create secret generic backend-secrets \
  --namespace=quarkus-crud \
  --from-file=private-key.pem=src/main/resources/keys/private-key.pem \
  --from-file=public-key.pem=src/main/resources/keys/public-key.pem
```

## Ingress Setup (Optional)

### Install Ingress Controller

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/cloud/deploy.yaml
```

### Deploy Ingress

```bash
kubectl apply -f manifests/ingress.yaml
```

Configure DNS to point to the ingress IP:
```bash
kubectl get ingress -n quarkus-crud
```

## Health Checks

The deployment includes comprehensive health checks:

### Liveness Probe
- Endpoint: `/q/health/live`
- Initial delay: 30 seconds
- Period: 10 seconds
- Timeout: 5 seconds
- Failure threshold: 3

### Readiness Probe
- Endpoint: `/q/health/ready`
- Initial delay: 10 seconds
- Period: 5 seconds
- Timeout: 3 seconds
- Failure threshold: 3

### Startup Probe
- Endpoint: `/q/health/started`
- Initial delay: 0 seconds
- Period: 5 seconds
- Timeout: 3 seconds
- Failure threshold: 30

## Scaling

### Manual Scaling

```bash
kubectl scale deployment backend \
  --namespace=quarkus-crud \
  --replicas=3
```

### Horizontal Pod Autoscaler

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: backend-hpa
  namespace: quarkus-crud
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: backend
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

Apply:
```bash
kubectl apply -f hpa.yaml
```

## Monitoring

### Logs

View application logs:
```bash
kubectl logs -f deployment/backend -n quarkus-crud
```

Stream logs:
```bash
kubectl logs -f deployment/backend -n quarkus-crud --tail=100
```

### Metrics

Quarkus provides metrics at:
- `/q/metrics`

Integrate with Prometheus for monitoring.

### Health Status

Check health:
```bash
kubectl exec -it deployment/backend -n quarkus-crud -- curl localhost:8080/healthz
```

## Backup and Recovery

### Database Backup

CloudNativePG handles automatic backups. Configure backup in the cluster spec:

```yaml
spec:
  backup:
    barmanObjectStore:
      destinationPath: s3://backup-bucket/
      s3Credentials:
        accessKeyId:
          name: backup-creds
          key: ACCESS_KEY_ID
        secretAccessKey:
          name: backup-creds
          key: ACCESS_SECRET_KEY
```

### Manual Backup

```bash
kubectl cnpg backup postgres -n quarkus-crud
```

### Restore

```bash
kubectl cnpg restore postgres --backup <backup-name> -n quarkus-crud
```

## Rolling Updates

Update application version:

```bash
kubectl set image deployment/backend \
  backend=ghcr.io/yuki-js/quarkus-crud:v0.0.2 \
  -n quarkus-crud
```

Monitor rollout:
```bash
kubectl rollout status deployment/backend -n quarkus-crud
```

Rollback if needed:
```bash
kubectl rollout undo deployment/backend -n quarkus-crud
```

## Troubleshooting

### Pod Not Starting

Check events:
```bash
kubectl describe pod <pod-name> -n quarkus-crud
```

Check logs:
```bash
kubectl logs <pod-name> -n quarkus-crud
```

### Database Connection Issues

Verify database service:
```bash
kubectl get service postgres-rw -n quarkus-crud
```

Test connection from pod:
```bash
kubectl exec -it deployment/backend -n quarkus-crud -- \
  sh -c 'echo "SELECT 1" | psql $QUARKUS_DATASOURCE_JDBC_URL'
```

### Resource Issues

Check resource usage:
```bash
kubectl top pods -n quarkus-crud
```

Adjust limits in deployment spec if needed.

## Security

### Network Policies

Restrict traffic:

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: backend-policy
  namespace: quarkus-crud
spec:
  podSelector:
    matchLabels:
      app: quarkus-crud
      component: backend
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: ingress-nginx
    ports:
    - protocol: TCP
      port: 8080
  egress:
  - to:
    - podSelector:
        matchLabels:
          app: postgres
    ports:
    - protocol: TCP
      port: 5432
```

### Pod Security Standards

Apply security context:

```yaml
securityContext:
  runAsNonRoot: true
  runAsUser: 1000
  fsGroup: 1000
  seccompProfile:
    type: RuntimeDefault
  capabilities:
    drop:
    - ALL
```

## Production Checklist

Before deploying to production:

- [ ] Set strong database password
- [ ] Configure TLS/SSL for database
- [ ] Set up ingress with TLS
- [ ] Configure monitoring and alerting
- [ ] Set up log aggregation
- [ ] Configure automatic backups
- [ ] Test disaster recovery
- [ ] Set resource limits and requests
- [ ] Configure network policies
- [ ] Enable pod security policies
- [ ] Set up horizontal pod autoscaling
- [ ] Test rolling updates
- [ ] Document runbooks
- [ ] Set up CI/CD pipeline

## CI/CD Integration

### GitHub Actions

See `.github/workflows/publish-jib.yml` for automated builds and deployments.

The workflow:
1. Builds the application
2. Runs tests
3. Builds container image
4. Pushes to container registry
5. Updates Kubernetes deployment (optional)

## Multi-Environment Setup

Use Kustomize for managing multiple environments:

```
manifests/
├── base/
│   ├── kustomization.yaml
│   ├── backend.yaml
│   └── database.yaml
└── overlays/
    ├── dev/
    │   └── kustomization.yaml
    ├── staging/
    │   └── kustomization.yaml
    └── prod/
        └── kustomization.yaml
```

Deploy to specific environment:
```bash
kubectl apply -k manifests/overlays/prod
```

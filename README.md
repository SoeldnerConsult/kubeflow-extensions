# disclaimer
this is a *research project*. This is more of a prototype;
This is not long-lasting software and may be obsolete on the next
version of kubeflow, therefore this piece of software is
missing substantial quality control measures;
i.e. we wrote some tests, but only to speed up development.
It's more of a short-term fix. :)

#use-case of kubeflow-extensions
Kubeflow has partial multi tenancy support, we increase the support
to katib and kubeflow pipelines (more or less ...)

Additionally, we fix some integration issues with jupyter notebooks.
These Pods may access the ml-pipeline; which results in errors.

## What is done and why
1) listen to new namespaces
5) pod admission controller - for python notebooks
3) trials admission controller
4) job admission controller
5) pod admission controller - for katib

### 1 listen to new namespaces, why?
When a new namespace is created, we need to create a new ServiceRoleBinding for the new namespace
allowing partial access to kubeflow pipelines out of the new namespace from pods via ist.io-sidecars

(this is only done, when a corresponding profile does exist)

Additionally, on namespace creation we need to create an EnvoyFilter,
allowing the full access (and therefore usability) of kubeflow pipelines out from other
namespaces.
(based on this issue https://github.com/kubeflow/pipelines/issues/4440#issuecomment-687703390)

The EnvoyFilter will utilize a label (e.g. kubeflow-extension = true) for identification of access.
The EnvoyFilter will only allow access to the namespace owner.
Another user with granted access won't be enabled to run any ml-pipeline or katib code

### 2) pod admission controller for python notebooks, why?
when a new pod gets created, we inject a label "kubeflow-extension = true", therefore
allowing this pod access via ist.io-sidecar to ml-pipelines (EnvoyFilter will
identify this sidecar, and therefore allow access to ml-pipelines api

### 3) trials admission controller, why
when a new trial gets created, we need to check whether modification is necessary.
When a trial gets started from within katib, they'll run in the kubeflow namespace (at this
point in time (12.10.2020)). Modification of these trials is unnecessary.

Is the trial not created in the kubeflow namespace, we add a kubeflow-extension=true label for identification
in following steps (of corresponding jobs and pods).

### 4) job admission controller, why?
- tag the pod with kubeflow-extension label (for identification and access through envoy filter) 
- additionally we change the service account to default-editor (because the default editor
most definitely has started this job, and the pipeline-runner SA does not have enough
rights for access through RBAC of Envoy / istio)
- finally we set the istio sidecar injection label.
doing this in the pod is too late - the pod has already run through the istio webhook

### 5) pod admission controller, why?
multiple containers in the pod need small behaviour changes

three containers exist in the created pod.
1) experiment executor container
2) katib metrics collector container
3) istio container

1) the experiment pod must sleep some seconds initially, or the istio proxy is not yet working.
unfortunately some extensions (kale); aren't that robust
2) the katib metrics collector container must kill the istio container after execution

In the second step we'll find additional problems, regarding exit codes etc.. this is explained
in detail within the code.

## limitations
This whole ordeal is experimental. We tried to run kubeflow in vSphere within a TKG cluster.
While we hit some issues, we tried to fix it, so we can play a bit with this technology within our lab.

Not everything is working perfectly. This may be fixed by kubeflow in the future, it's
enough for us to explore the full capabilities of kubeflow to an extent not yet visible in any other project.

1) experiments suddenly fail without reason
> this is a limitation by the utilized ist.io version (1.3)
> Unfortunately there does not seem to be a fix. 
> Although, we found a symptom mitigation strategy (error does happen substantially less)
> we think it's because of the following issue
> https://github.com/istio/istio/issues/17383

2) experiments are not finishing
> Experiments just stop running after a while
> the reason is unknown. It may happens because of some fixes we introduce or not.
> Sometimes they complete, sometimes not
 

#installation
1. modify build
2. mvn package
3. ./installation.sh
```shell script
#setup namespace
kubectl create ns kubeflow-extension
kubectl config set-context $(kubectl config current-context) --namespace=kubeflow-extension

#push docker image
mvn package -DskipTests
docker build -f src/main/docker/Dockerfile.jvm -t keeyzar/tenancy-fixer-jvm .
docker push keeyzar/tenancy-fixer-jvm

#create certifcate request and sign..
#make sure, go; cfssl cfssljson are installed
chmod +x create-cert.sh
./create-cert.sh

#deploy tenancy-fixer
kubectl apply -f tenancy-fixer.yaml
mutator=$(kubectl get pods --selector=app=tenancy-fixer -ojsonpath='{.items[*].metadata.name}')
kubectl wait --for=condition=Ready --timeout=300s pod/$mutator

#create test namespace plus deployment, for testing access against own service via ssl
kubectl create ns test-admission
kubectl label ns test-admission admission=enabled
kubectl apply -f httpbin.yaml

sleep 5
httpbin=$(kubectl get pods -n test-admission --selector=name=httpbin -o jsonpath='{.items[*].metadata.name}')
kubectl -n test-admission wait --for=condition=Ready --timeout=300s pod/$httpbin
kubectl exec $httpbin -n test-admission -- curl \
    --cacert /var/run/secrets/kubernetes.io/serviceaccount/ca.crt  \
    --request POST \
    -H "Content-Type: application/json" \
    --data '{"additionalProperties":{},"apiVersion":"admission.k8s.io/v1beta1","kind":"AdmissionReview","request":{"additionalProperties":{},"kind":{"additionalProperties":{},"group":"extensions","kind":"Deployment","version":"v1beta1"},"name":"httpbin","namespace":"test-admission","operation":"UPDATE","resource":{"additionalProperties":{},"group":"extensions","resource":"deployments","version":"v1beta1"},"uid":"75a55056-bc03-11e9-82d4-025000000001","userInfo":{"additionalProperties":{},"groups":["system:masters","system:authenticated"],"username":"docker-for-desktop"}}}' \
    https://tenancy-fixer.kubeflow-extension.svc/pod/mutate

#obtain certificate from pod: 
controller=$(kubectl get pods --selector=app=tenancy-fixer -o jsonpath='{.items[*].metadata.name}')
cert=$(kubectl exec $controller -- cat /var/run/secrets/kubernetes.io/serviceaccount/ca.crt | base64 | tr -d '\n')
sed -i.bak -E "s/caBundle:.*?/caBundle: $cert/" webhooks.yaml
kubectl apply -f webhooks.yaml


#done :)
```

# tenancy-fixer project

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```
./mvnw quarkus:dev
```

## Packaging and running the application

The application can be packaged using `./mvnw package`.
It produces the `tenancy-fixer-1.0-runner.jar` file in the `/target` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/lib` directory.

The application is now runnable using `java -jar target/tenancy-fixer-1.0-runner.jar`.

## Creating a native executable

You can create a native executable using: `./mvnw package -Pnative`.

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: `./mvnw package -Pnative -Dquarkus.native.container-build=true`.

You can then execute your native executable with: `./target/tenancy-fixer-1.0-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/building-native-image.
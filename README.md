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
allowing partial access to kubeflow pipelines out of the new namespace from pods via istio-sidecars

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
allowing this pod access via istio-sidecar to ml-pipelines (EnvoyFilter will
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
1. apply k8s resources for NS, RBAC
2. build & push docker image
3. apply k8s resource for deployment
4. create a certificate for admission review registration
5. apply k8s resource for admission webhooks

```shell script
#setup k8s resources
#ns, role, role binding for psp and role binding for api-server access
kubectl apply -f k8s-resources/resources.yaml

#build package and push docker image
mvn package -DskipTests
docker build -f src/main/docker/Dockerfile.jvm -t keeyzar/tenancy-fixer-jvm .
docker push keeyzar/tenancy-fixer-jvm

#create certificate request and sign..
#make sure, go; cfssl cfssljson are installed
chmod +x create-cert.sh
./create-cert.sh

#deploy tenancy-fixer
kubectl apply -f tenancy-fixer.yaml
mutator=$(kubectl get pods --selector=app=tenancy-fixer -ojsonpath='{.items[*].metadata.name}')
kubectl wait --for=condition=Ready --timeout=300s pod/$mutator

#obtain certificate from pod, which the api-server should utilize as public-key 
controller=$(kubectl get pods --selector=app=tenancy-fixer -o jsonpath='{.items[*].metadata.name}')
cert=$(kubectl exec $controller -- cat /var/run/secrets/kubernetes.io/serviceaccount/ca.crt | base64 | tr -d '\n')
sed -i.bak -E "s/caBundle:.*?/caBundle: $cert/" webhooks.yaml
kubectl apply -f webhooks.yaml


#done :)
```

## debugging
for debugging purpose get logs of tenancy-fixer-* in kubeflow-extension namespace
```
k logs -n kubeflow-extension --selector=app=tenancy-fixer --tail=-1
```
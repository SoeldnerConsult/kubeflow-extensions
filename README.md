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
docker build -f src/main/docker/Dockerfile.jvm -t keeyzar/trials-mutator-jvm .
docker push keeyzar/trials-mutator-jvm

#create certifcate request and sign..
#make sure, go; cfssl cfssljson are installed
chmod +x create-cert.sh
./create-cert.sh

#deploy trials-mutator
kubectl apply -f trials-mutator.yaml
mutator=$(kubectl get pods --selector=app=trials-mutator -ojsonpath='{.items[*].metadata.name}')
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
    https://trials-mutator.kubeflow-extension.svc/validate

#obtain certificate from pod: 
controller=$(kubectl get pods --selector=app=trials-mutator -o jsonpath='{.items[*].metadata.name}')
cert=$(kubectl exec $controller -- cat /var/run/secrets/kubernetes.io/serviceaccount/ca.crt | base64 | tr -d '\n')
sed -i.bak -E "s/caBundle:.*?/caBundle: $cert/" validation-webhook.yaml
kubectl apply -f validation-webhook.yaml


#done :)
```

# trials-mutator project

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```
./mvnw quarkus:dev
```

## Packaging and running the application

The application can be packaged using `./mvnw package`.
It produces the `trials-mutator-1.0-runner.jar` file in the `/target` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/lib` directory.

The application is now runnable using `java -jar target/trials-mutator-1.0-runner.jar`.

## Creating a native executable

You can create a native executable using: `./mvnw package -Pnative`.

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: `./mvnw package -Pnative -Dquarkus.native.container-build=true`.

You can then execute your native executable with: `./target/trials-mutator-1.0-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/building-native-image.
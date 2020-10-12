#!/bin/zsh

mkdir -p target/cert
cp csr.json target/cert
pushd target/cert

# Create private key and CSR
cfssl genkey csr.json | cfssljson -bare trials-mutator

# Create CSR k8s object
cat <<EOF | kubectl create -f -
apiVersion: certificates.k8s.io/v1beta1
kind: CertificateSigningRequest
metadata:
  name: trials-mutator
spec:
  groups:
  - system:authenticated
  request: $(cat trials-mutator.csr | base64 | tr -d '\n')
  usages:
  - digital signature
  - key encipherment
  - server auth
EOF

# Approve certificate
kubectl certificate approve trials-mutator

sleep 5s

# Download public key
kubectl get csr trials-mutator -o jsonpath='{.status.certificate}' | base64 --decode > trials-mutator.crt

cp trials-mutator-key.pem tls.key
cp trials-mutator.crt tls.crt
kubectl create secret tls trials-mutator-tls -n kubeflow-extension --key ./tls.key --cert ./tls.crt

# Display public key content
openssl x509 -in tls.crt -text
  #Propriétaire : CN=trials-mutator.kubeflow-extension.svc
  #Emetteur : CN=kubernetes

popd

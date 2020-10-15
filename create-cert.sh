#!/bin/zsh

mkdir -p target/cert
cp csr.json target/cert
pushd target/cert

# Create private key and CSR
cfssl genkey csr.json | cfssljson -bare tenancy-fixer

# Create CSR k8s object
cat <<EOF | kubectl create -f -
apiVersion: certificates.k8s.io/v1beta1
kind: CertificateSigningRequest
metadata:
  name: tenancy-fixer
spec:
  groups:
  - system:authenticated
  request: $(cat tenancy-fixer.csr | base64 | tr -d '\n')
  usages:
  - digital signature
  - key encipherment
  - server auth
EOF

# Approve certificate
kubectl certificate approve tenancy-fixer

sleep 5s

# Download public key
kubectl get csr tenancy-fixer -o jsonpath='{.status.certificate}' | base64 --decode > tenancy-fixer.crt

cp tenancy-fixer-key.pem tls.key
cp tenancy-fixer.crt tls.crt
kubectl create secret tls tenancy-fixer-tls -n kubeflow-extension --key ./tls.key --cert ./tls.crt

# Display public key content
openssl x509 -in tls.crt -text
  #Propriétaire : CN=tenancy-fixer.kubeflow-extension.svc
  #Emetteur : CN=kubernetes

popd

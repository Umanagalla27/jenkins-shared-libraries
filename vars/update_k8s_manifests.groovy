#!/usr/bin/env groovy

def call(Map config = [:]) {
    def imageTag       = config.imageTag ?: error("Image tag is required")
    def manifestsPath  = config.manifestsPath ?: 'kubernetes'
    def gitCredentials = config.gitCredentials ?: 'github-credentials'
    def gitUserName    = config.gitUserName ?: 'Jenkins CI'
    def gitUserEmail   = config.gitUserEmail ?: 'jenkins@example.com'
    def ingressHost    = config.ingressHost ?: 'easyshop.letsdeployit.com'

    // Images and branch resolved dynamically, with sane defaults
    def mainImage      = (config.mainImage ?: env.DOCKER_IMAGE_NAME) ?: 'uma1827/easyshop-app'
    def migrationImage = (config.migrationImage ?: env.DOCKER_MIGRATION_IMAGE_NAME) ?: 'uma1827/easyshop-migration'
    def branch         = (config.branch ?: env.GIT_BRANCH) ?: 'master'

    // Target repo (owner/repo). Override via config.repoSlug if needed
    def repoSlug       = config.repoSlug ?: 'Umanagalla27/tws-e-commerce-app_hackathon'

    echo "Updating Kubernetes manifests with image tag: ${imageTag}"

    withCredentials([usernamePassword(
        credentialsId: gitCredentials,
        usernameVariable: 'GIT_USERNAME',
        passwordVariable: 'GIT_PASSWORD'
    )]) {
        sh """
          set -e

          git config user.name "${gitUserName}"
          git config user.email "${gitUserEmail}"

          # Update main app image
          sed -i "s|^\\s*image: .*$|image: ${mainImage}:${imageTag}|g" ${manifestsPath}/08-easyshop-deployment.yaml

          # Update migration job image if present
          if [ -f "${manifestsPath}/12-migration-job.yaml" ]; then
            sed -i "s|^\\s*image: .*$|image: ${migrationImage}:${imageTag}|g" ${manifestsPath}/12-migration-job.yaml
          fi

          # Ensure ingress host if present
          if [ -f "${manifestsPath}/10-ingress.yaml" ]; then
            sed -i "s|^\\s*host: .*$|host: ${ingressHost}|g" ${manifestsPath}/10-ingress.yaml
          fi

          # Stage and commit only if there are changes
          git add ${manifestsPath}/*.yaml || true
          git diff --cached --quiet || git commit -m "Update image tags to ${imageTag} [ci skip]" || true

          # Push using authenticated HTTPS without resetting 'origin'
          git push "https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/${repoSlug}.git" HEAD:${branch}
        """
    }
}

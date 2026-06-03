FROM eclipse-temurin:8-jdk-jammy AS builder

WORKDIR /build

ARG MAVEN_VERSION=3.9.6

RUN apt-get update && apt-get install -y --no-install-recommends wget git \
    && rm -rf /var/lib/apt/lists/* \
    && wget -q "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz" \
    && tar -xzf apache-maven-${MAVEN_VERSION}-bin.tar.gz -C /opt \
    && ln -s /opt/apache-maven-${MAVEN_VERSION}/bin/mvn /usr/local/bin/mvn \
    && rm apache-maven-${MAVEN_VERSION}-bin.tar.gz

COPY ./openmrs-module-appointmentscheduling /build/appointmentscheduling
RUN cd /build/appointmentscheduling && mvn clean package -DskipTests -q

# Download OpenMRS WAR in builder so the runtime stage stays lean
RUN mvn dependency:get \
      -DremoteRepositories=https://mavenrepo.openmrs.org/public \
      -Dartifact=org.openmrs.web:openmrs:2.3.1:war \
      -Ddest=/build/openmrs.war

# ───────────────────────────────────────────────────────────────
FROM eclipse-temurin:8-jre-jammy AS runtime

ARG BUILD_ENV=development
ARG TOMCAT_VERSION=9.0.89

ENV BUILD_ENV=${BUILD_ENV} \
    CATALINA_HOME=/opt/tomcat \
    PATH=/opt/tomcat/bin:$PATH

WORKDIR /opt/openmrs

RUN apt-get update && apt-get install -y --no-install-recommends curl wget ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Install Tomcat 9 from Apache archive (no apt to avoid Java version conflicts)
RUN wget -q "https://archive.apache.org/dist/tomcat/tomcat-9/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz" \
    && tar -xzf apache-tomcat-${TOMCAT_VERSION}.tar.gz -C /opt \
    && mv /opt/apache-tomcat-${TOMCAT_VERSION} /opt/tomcat \
    && rm apache-tomcat-${TOMCAT_VERSION}.tar.gz \
    && chmod +x /opt/tomcat/bin/*.sh \
    && rm -rf /opt/tomcat/webapps/ROOT /opt/tomcat/webapps/examples \
              /opt/tomcat/webapps/host-manager /opt/tomcat/webapps/manager

COPY --from=builder /build/openmrs.war /opt/tomcat/webapps/openmrs.war

RUN mkdir -p /opt/openmrs/modules /opt/openmrs/data /opt/openmrs/logs

# Copy built module (overridden by volume mount in development)
COPY --from=builder /build/appointmentscheduling/omod/target/*.omod /opt/openmrs/modules/

# Set JVM options per environment; docker-compose env vars override at runtime
RUN if [ "$BUILD_ENV" = "production" ]; then \
        printf 'export JAVA_OPTS="-Xmx2048m -Xms1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Djava.security.egd=file:/dev/./urandom"\n' \
          > /opt/tomcat/bin/setenv.sh; \
    elif [ "$BUILD_ENV" = "development" ]; then \
        printf 'export JAVA_OPTS="-Xmx512m -Xms256m -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=*:5005 -Djava.security.egd=file:/dev/./urandom"\n' \
          > /opt/tomcat/bin/setenv.sh; \
    else \
        printf 'export JAVA_OPTS="-Xmx1024m -Xms512m -Djava.security.egd=file:/dev/./urandom"\n' \
          > /opt/tomcat/bin/setenv.sh; \
    fi && chmod +x /opt/tomcat/bin/setenv.sh

# Create non-root user; docker-compose.prod.yml sets `user: openmrs`
RUN groupadd -r openmrs && useradd -r -g openmrs openmrs \
    && chown -R openmrs:openmrs /opt/openmrs \
    && chown -R openmrs:openmrs /opt/tomcat/logs /opt/tomcat/work \
                                /opt/tomcat/temp /opt/tomcat/webapps

EXPOSE 8080
EXPOSE 5005

HEALTHCHECK --interval=30s --timeout=10s --start-period=180s --retries=5 \
    CMD curl -f http://localhost:8080/openmrs/ || exit 1

CMD ["/opt/tomcat/bin/catalina.sh", "run"]

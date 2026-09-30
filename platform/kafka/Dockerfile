FROM apache/kafka:4.0.0
USER root
COPY platform/kafka/entrypoint.sh /opt/kafka/custom-entrypoint.sh
COPY platform/kafka/init-topics-acls.sh /opt/kafka/init-topics-acls.sh
RUN chmod +x /opt/kafka/custom-entrypoint.sh /opt/kafka/init-topics-acls.sh
ENTRYPOINT ["/opt/kafka/custom-entrypoint.sh"]

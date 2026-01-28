FROM docker.elastic.co/elasticsearch/elasticsearch:8.11.3

# Install Chinese analyzer (official plugin)
# NOTE: IK does not provide Elasticsearch 8.x release artifacts reliably; smartcn is stable and good enough.
RUN elasticsearch-plugin install --batch analysis-smartcn

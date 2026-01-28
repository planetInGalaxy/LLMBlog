FROM docker.elastic.co/elasticsearch/elasticsearch:8.11.3

# Install IK analyzer for Chinese
# NOTE: Version must match Elasticsearch version.
RUN elasticsearch-plugin install --batch https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v8.11.3/elasticsearch-analysis-ik-8.11.3.zip

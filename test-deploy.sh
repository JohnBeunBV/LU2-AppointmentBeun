#!/bin/bash
# test-deploy.sh - Local test of deployment processes

set -e

echo "🧪 Starting test deployment simulation..."

# Load test environment
export $(cat .env.test | xargs)

# Build test image
echo "🔨 Building test image..."
docker build -f .docker/Dockerfile.test -t openmrs-appointment:test .

# Start test services
echo "🐳 Starting test services..."
docker-compose -f docker-compose.test.yml --env-file .env.test up -d

# Wait for services
echo "⏳ Waiting for services (60 seconds)..."
for i in {1..60}; do
    if docker-compose -f docker-compose.test.yml exec -T openmrs curl -f http://localhost:8080/openmrs/ > /dev/null 2>&1; then
        echo "✅ Services are ready after $i seconds"
        break
    fi
    [ $((i % 10)) -eq 0 ] && echo "   Still waiting... ($i/60)"
    sleep 1
done

# Run tests
echo "🧪 Running module tests..."
docker-compose -f docker-compose.test.yml exec -T openmrs bash -c \
    "cd /opt/openmrs/modules && mvn test"

# Collect results
echo "📊 Test Results:"
if [ $? -eq 0 ]; then
    echo "✅ All tests passed!"
else
    echo "❌ Tests failed"
fi

# Cleanup
echo ""
read -p "Cleanup test environment? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    docker-compose -f docker-compose.test.yml down -v
    echo "🧹 Cleanup complete"
fi

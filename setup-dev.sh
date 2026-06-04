#!/bin/bash
# setup-dev.sh - Quick setup for local development

set -e

echo "🚀 Setting up development environment..."

# Check prerequisites
if ! command -v docker &> /dev/null; then
    echo "❌ Docker not installed"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose not installed"
    exit 1
fi

# Create .env if not exists
if [ ! -f .env.dev ]; then
    echo "📝 Creating .env.dev..."
    cp .env.dev .env.dev  # Already exists from template
fi

# Build the module first
echo "🔨 Building Appointment Scheduling module..."
cd openmrs-module-appointmentscheduling
mvn clean install -DskipTests
cd ..

# Start services
echo "🐳 Starting Docker services..."
docker-compose -f docker-compose.dev.yml up -d

# Wait for services
echo "⏳ Waiting for services to be ready..."
sleep 20

# Check health
echo "🏥 Checking health..."
if docker-compose -f docker-compose.dev.yml exec -T openmrs curl -f http://localhost:8080/openmrs/ > /dev/null 2>&1; then
    echo "✅ OpenMRS is ready!"
    echo ""
    echo "📍 Access OpenMRS:"
    echo "   URL: http://localhost:8080/openmrs"
    echo "   User: admin"
    echo "   Password: Admin123"
else
    echo "❌ OpenMRS failed to start"
    docker-compose -f docker-compose.dev.yml logs openmrs
    exit 1
fi

echo ""
echo "✨ Development environment is ready!"
echo ""
echo "Useful commands:"
echo "  docker-compose -f docker-compose.dev.yml logs -f openmrs"
echo "  docker-compose -f docker-compose.dev.yml down"

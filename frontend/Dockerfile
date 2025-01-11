# Stage 1: Build the React app
FROM node:16 AS build
WORKDIR /app

# Copy package.json and package-lock.json
COPY package.json package-lock.json ./

COPY .env .env

# Install dependencies
RUN npm install

# Copy the application source code
COPY . .

# Build the React application for production
RUN npm run build

# Stage 2: Serve the app with Nginx
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Remove default Nginx static files
RUN rm -rf ./*

# Copy the React build output from the previous stage
COPY --from=build /app/build .

# Expose port 80 for Nginx
EXPOSE 80

# Start Nginx server
CMD ["nginx", "-g", "daemon off;"]
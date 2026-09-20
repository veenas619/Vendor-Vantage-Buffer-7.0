FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY . .
RUN find . -name "*.java" > sources.txt && javac -d out @sources.txt
EXPOSE 8080
CMD ["java", "-cp", "out", "Server"]

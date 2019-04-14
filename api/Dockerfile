FROM hseeberger/scala-sbt AS base
WORKDIR /root
EXPOSE 9000
COPY . .
RUN sbt assembly
CMD ["sbt", "run"]

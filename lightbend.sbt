resolvers in ThisBuild += "lightbend-commercial-mvn" at
  "https://repo.lightbend.com/pass/TJSw8aUXzCyP-6M2fvR0NppbZCJOmhAdpmZzE_a8pbsQAEtF/commercial-releases"
resolvers in ThisBuild += Resolver.url("lightbend-commercial-ivy",
  url("https://repo.lightbend.com/pass/TJSw8aUXzCyP-6M2fvR0NppbZCJOmhAdpmZzE_a8pbsQAEtF/commercial-releases"))(Resolver.ivyStylePatterns)

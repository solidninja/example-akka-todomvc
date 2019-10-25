# TodoMVC using Akka{,Typed,Persistence} and Tapir

Sandbox repo containing a Todo list app implemented using:

  * [akka-http]
  * [akka-persistence-typed]
  * [tapir]
  
The codebase is based on [example-pure-todomvc].

## Missing features FIXME

* GUI is not implemented
* No tests for REST API client

## Running

`sbt todomvc-server/run` and navigate to http://127.0.0.1:9090/docs to see the Swagger UI.

## License

This project is licensed as [MIT][mit-license].

[akka-http]: https://github.com/akka/akka-http
[akka-persistence-typed]: https://doc.akka.io/docs/akka/2.6/typed/persistence.html
[example-pure-todomvc]: https://gitlab.com/solidninja/example-pure-todomvc
[mit-license]: https://opensource.org/licenses/MIT
[tapir]: https://github.com/softwaremill/tapir

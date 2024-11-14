$version: "2.0"

namespace fullstack_scala.protocol

use alloy#simpleRestJson
use alloy#uuidFormat

@simpleRestJson
service TestService {
  version: "1.0.0",
  operations: [ListTests, CreateTest]
}

@readonly
@http(method: "GET", uri: "/api/tests", code: 200)
operation ListTests {
  output := {
    @required
    tests: Tests
  }
}

@idempotent
@http(method: "PUT", uri: "/api/test", code: 200)
operation CreateTest {
  
  input := {
    @required
    attributes: TestAttributes
  }

  output := {
    @required
    test: Test
  }
}



list Tests {
  member: Test
}

structure Test {
  @required
  id: TestId

  @required
  attributes: TestAttributes

  deleted: Boolean = false
}

structure TestAttributes {
  @required
  title: TestTitle

  description: TestDescription
}

integer TestId
string TestTitle
string TestDescription

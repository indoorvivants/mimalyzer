$version: "2.0"

namespace mimalyzer.protocol

use alloy#simpleRestJson
use alloy#uuidFormat

@simpleRestJson
service MimaService {
    version: "1.0.0"
    operations: [
        Health
        GetComparison
        CreateComparison
        GetStatus
    ]
}

@readonly
@http(method: "GET", uri: "/api/health", code: 200)
operation Health {
    output := {
        @required
        status: String
    }
}

@readonly
@http(method: "GET", uri: "/api/status/{id}", code: 200)
operation GetStatus {
    input := {
        @required
        @httpLabel
        id: ComparisonId
    }

    output := {
        @required
        status: ComparisonStatus
    }
}

@readonly
@http(method: "GET", uri: "/api/comparison/{id}", code: 200)
operation GetComparison {
    input := {
        @required
        @httpLabel
        id: ComparisonId
    }

    output := {
        @required
        comparison: Comparison
    }

    errors: [
        NotFound
    ]
}

structure Waiting {}

structure Completed {}

@error("client")
@httpError(400)
structure NotFound {}

structure Processing {
    step: ProcessingStep
    remaining: Integer
}

@error("client")
@httpError(400)
structure CompilationFailed {
    @required
    which: CodeLabel

    @required
    errorOut: String
}

union ComparisonStatus {
    waiting: Waiting
    processing: Processing
    failed: CompilationFailed
    completed: Completed
    notFound: NotFound
}

@idempotent
@http(method: "PUT", uri: "/api/comparison", code: 200)
operation CreateComparison {
    input := {
        @required
        attributes: ComparisonAttributes
    }

    output := {
        @required
        comparisonId: ComparisonId
    }

    errors: [
        CodeTooBig
        InvalidScalaVersion
    ]
}

structure MimaProblems {
    @required
    problems: ProblemsList
}

structure TastyMimaProblems {
    @required
    problems: ProblemsList
}

@error("client")
@httpError(400)
structure InvalidScalaVersion {}

@error("client")
@httpError(400)
structure CodeTooBig {
    @required
    sizeBytes: Integer

    @required
    maxSizeBytes: Integer

    @required
    which: CodeLabel
}

enum CodeLabel {
    AFTER
    BEFORE
}

list ProblemsList {
    member: Problem
}

structure Problem {
    @required
    message: String
}

structure Comparison {
    @required
    id: ComparisonId

    @required
    attributes: ComparisonAttributes

    mimaProblems: MimaProblems

    tastyMimaProblems: TastyMimaProblems
}

structure ComparisonAttributes {
    @required
    beforeScalaCode: ScalaCode

    @required
    afterScalaCode: ScalaCode

    @required
    scalaVersion: ScalaVersion
}

@uuidFormat
string ComparisonId

string ScalaCode

enum ScalaVersion {
    SCALA_212 = "2.12"
    SCALA_213 = "2.13"
    SCALA_3_LTS = "3 LTS"
}

enum ProcessingStep {
    PICKED_UP = "picked-up"
    CODE_BEFORE_COMPILED = "code-before-compiled"
    CODE_AFTER_COMPILED = "code-after-compiled"
    MIMA_FINISHED = "mima-finished"
    TASTY_MIMA_FINISHED = "tasty-mima-finished"
}

package com.minosiants

import cats.data.Kleisli
import cats.effect.IO

package object pencil {

  type Smtp[A] = Kleisli[IO, Request, A]
}

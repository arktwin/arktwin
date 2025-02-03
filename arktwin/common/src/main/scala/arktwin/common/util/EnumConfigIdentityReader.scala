// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.common.util

import pureconfig.generic.derivation.EnumConfigReaderDerivation

type EnumConfigIdentityReader[A] = EnumConfigIdentityReaderDerivation.EnumConfigReader[A]

object EnumConfigIdentityReaderDerivation extends EnumConfigReaderDerivation(identity)

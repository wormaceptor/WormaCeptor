# Changelog

All notable changes to WormaCeptor will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.5.1](https://github.com/wormaceptor/WormaCeptor/compare/v3.5.0...v3.5.1) (2026-11-13)


### CI/CD

* **release:** configure git credentials before pushing the website pr ([#83](https://github.com/wormaceptor/WormaCeptor/issues/83)) ([4866e7d](https://github.com/wormaceptor/WormaCeptor/commit/4866e7d79b69951405ac718d28a5600d2ed163c3))

## [3.5.0](https://github.com/wormaceptor/WormaCeptor/compare/v3.4.4...v3.5.0) (2026-11-12)


### Features

* host integration options and adb-only mcp socket ([#80](https://github.com/wormaceptor/WormaCeptor/issues/80)) ([c141bae](https://github.com/wormaceptor/WormaCeptor/commit/c141bae4a30fb63351abc3355782fd7f8eba3574))


### Bug Fixes

* **mcp:** avoid api 24 concurrent set in the adb socket proxy ([#82](https://github.com/wormaceptor/WormaCeptor/issues/82)) ([13f3201](https://github.com/wormaceptor/WormaCeptor/commit/13f3201a7d93e4a825f50b25ed1fede995f9319c))

## [3.4.4](https://github.com/wormaceptor/WormaCeptor/compare/v3.4.3...v3.4.4) (2026-11-10)


### CI/CD

* **release:** add manual workflow to promote the demo app from internal ([#77](https://github.com/wormaceptor/WormaCeptor/issues/77)) ([498cb4f](https://github.com/wormaceptor/WormaCeptor/commit/498cb4f112d817d0d197bcaee8ae6bac0a9115e6))

## [3.4.3](https://github.com/wormaceptor/WormaCeptor/compare/v3.4.2...v3.4.3) (2026-11-10)


### Bug Fixes

* **build:** take convention plugin dependency versions from the catalog ([#75](https://github.com/wormaceptor/WormaCeptor/issues/75)) ([48e9162](https://github.com/wormaceptor/WormaCeptor/commit/48e9162cb9d6e09e433924ba149dd73499e26f35))
* **mcp:** run the device server on ktor's cio engine ([#76](https://github.com/wormaceptor/WormaCeptor/issues/76)) ([2e7fd5a](https://github.com/wormaceptor/WormaCeptor/commit/2e7fd5a28849476b8b14f6a5f59f06b9990694dd))


### Build

* **publishing:** release maven central deployments automatically ([2300da5](https://github.com/wormaceptor/WormaCeptor/commit/2300da514731b551a6a6272a1340f2aa87442f37))
* **publishing:** release maven central deployments automatically ([d4a495d](https://github.com/wormaceptor/WormaCeptor/commit/d4a495dfdfebffe89a55fcd365d02b5cacbb2ec1))


### CI/CD

* **release:** open a website PR that bumps the version ([af2bedf](https://github.com/wormaceptor/WormaCeptor/commit/af2bedff1103df1bbae322f6be06bb4a86d67dd5))
* **release:** open a website PR that bumps the version ([7d63326](https://github.com/wormaceptor/WormaCeptor/commit/7d633269d1b776cbfddb79d95e5dbbc38b588a49))

## [3.4.2](https://github.com/wormaceptor/WormaCeptor/compare/v3.4.1...v3.4.2) (2026-10-18)


### Bug Fixes

* **app:** raise demo app minSdk to 24 and drop the dead website dispatch ([1d4e480](https://github.com/wormaceptor/WormaCeptor/commit/1d4e48087507df3954a55a148aba4beaad4adc83))
* **app:** raise demo app minSdk to 24 for play automatic protection ([998ca49](https://github.com/wormaceptor/WormaCeptor/commit/998ca4957e75785a70ece8d5478060a67609e9aa))


### Build

* **deps:** bump actions/checkout from 4 to 7 ([b488014](https://github.com/wormaceptor/WormaCeptor/commit/b488014b772d2880d08b29edc4d84cf66e71fb98))
* **deps:** bump actions/checkout from 4 to 7 ([1cbfebf](https://github.com/wormaceptor/WormaCeptor/commit/1cbfebfdca5d6534496163d6e3798a6456601da8))
* **deps:** bump actions/setup-node from 4 to 7 ([daeaa56](https://github.com/wormaceptor/WormaCeptor/commit/daeaa56418a3ada436498ad75b44a4d37efba788))
* **deps:** bump actions/setup-node from 4 to 7 ([da85fac](https://github.com/wormaceptor/WormaCeptor/commit/da85fac9437bed16377ab1dcb1178ef0f773f90e))
* **deps:** bump androidx.core:core-splashscreen from 1.0.1 to 1.2.0 ([46134d9](https://github.com/wormaceptor/WormaCeptor/commit/46134d991be1a95770b41dd27f154424cb1ba6c1))
* **deps:** bump androidx.core:core-splashscreen from 1.0.1 to 1.2.0 ([2557edb](https://github.com/wormaceptor/WormaCeptor/commit/2557edbb99f96190151ac6334c09e42428ca2006))
* **deps:** bump androidx.paging:paging-compose from 3.3.6 to 3.5.3 ([9647636](https://github.com/wormaceptor/WormaCeptor/commit/964763687a58f1975459e8565753c5b801848be9))
* **deps:** bump androidx.paging:paging-compose from 3.3.6 to 3.5.3 ([4f053f6](https://github.com/wormaceptor/WormaCeptor/commit/4f053f6b11ecc8f5ad0b881d9b41b3061dbd88c3))
* **deps:** bump app.cash.turbine:turbine from 1.2.0 to 1.2.1 ([99fb326](https://github.com/wormaceptor/WormaCeptor/commit/99fb326d2bc46e39e0c169546b43a950d79654bf))
* **deps:** bump app.cash.turbine:turbine from 1.2.0 to 1.2.1 ([f9ac0a2](https://github.com/wormaceptor/WormaCeptor/commit/f9ac0a2a09767ecfd5e9bde7a3ab934e1fe9dccf))
* **deps:** bump com.squareup.sort-dependencies from 0.6 to 0.21.0 ([4d7036e](https://github.com/wormaceptor/WormaCeptor/commit/4d7036e10fbac6a37df1d33d3397b813eb95a82a))
* **deps:** bump com.squareup.sort-dependencies from 0.6 to 0.21.0 ([2e5bf64](https://github.com/wormaceptor/WormaCeptor/commit/2e5bf64f886ea577e9ca5af7d396d1b1d612dd01))
* **deps:** bump googleapis/release-please-action from 4 to 5 ([da0d53c](https://github.com/wormaceptor/WormaCeptor/commit/da0d53c18a00bdd550546dbd9313609771c6aeb6))
* **deps:** bump googleapis/release-please-action from 4 to 5 ([7b1813e](https://github.com/wormaceptor/WormaCeptor/commit/7b1813ee4fd141a76a0dfb45b359c63b01f1d62f))
* **deps:** bump junit-jupiter to 6.1.3 ([24de347](https://github.com/wormaceptor/WormaCeptor/commit/24de3476831b9e19c411c4a8d93354b5a25bb256))
* **deps:** bump junit-jupiter to 6.1.3 and add the aligned platform launcher ([20ff102](https://github.com/wormaceptor/WormaCeptor/commit/20ff10257e8a2a2312ba2e178b74bc8da0911db8))
* **deps:** bump koin from 4.0.0 to 4.2.2 ([a55d560](https://github.com/wormaceptor/WormaCeptor/commit/a55d5604fa5d07a7d3a63cedf8fde651b4d5b5fd))
* **deps:** bump koin from 4.0.0 to 4.2.2 ([167670b](https://github.com/wormaceptor/WormaCeptor/commit/167670b63e3701bb632a8282dc89c134631dbad8))
* **deps:** bump kotlin from 2.4.20 to 2.4.21 ([7c9fc0d](https://github.com/wormaceptor/WormaCeptor/commit/7c9fc0dfbb0f428b96b7f240b89d1dc4891922d5))
* **deps:** bump kotlin from 2.4.20 to 2.4.21 ([6c9f888](https://github.com/wormaceptor/WormaCeptor/commit/6c9f8884f2678804004d1bbcff68ae103bfcb132))
* **deps:** bump ktor from 3.0.3 to 3.6.0 ([5d7997a](https://github.com/wormaceptor/WormaCeptor/commit/5d7997a9026b96bb8729bac114578420d8cf18fc))
* **deps:** bump ktor from 3.0.3 to 3.6.0 ([6df3f38](https://github.com/wormaceptor/WormaCeptor/commit/6df3f38391d2c104ec63a89d642f33f34c804fad))
* **deps:** bump org.jetbrains.kotlin.jvm from 2.4.20 to 2.4.21 ([9431e8c](https://github.com/wormaceptor/WormaCeptor/commit/9431e8ccd0b192859b9d632c5c3ad72669b09055))
* **deps:** bump org.jetbrains.kotlin.jvm from 2.4.20 to 2.4.21 ([80b458b](https://github.com/wormaceptor/WormaCeptor/commit/80b458b35eab370e8e572030ac4059857388a756))
* **deps:** bump retrofit from 2.9.0 to 3.0.0 ([384d678](https://github.com/wormaceptor/WormaCeptor/commit/384d6784db65811c8d0f09de60187463a58a0e66))
* **deps:** bump retrofit from 2.9.0 to 3.0.0 ([fef25ef](https://github.com/wormaceptor/WormaCeptor/commit/fef25ef42395bda417b605a5c76d983803eaa7ec))
* **lint:** ignore netty's javax.naming.ldap reference for ktor 3.6 ([7eacd33](https://github.com/wormaceptor/WormaCeptor/commit/7eacd334f29dfdf3ef280c605ebc81f0c4722a04))


### CI/CD

* pass gitleaks license to the secret scan ([0d9ef8e](https://github.com/wormaceptor/WormaCeptor/commit/0d9ef8e00bebe80794cdb28130d1dbabeb4bc027))
* **release:** drop the website dispatch job ([d81479f](https://github.com/wormaceptor/WormaCeptor/commit/d81479fba0c532f285ce16ab1de5ccc7ed9b4c1e))

## [3.4.1](https://github.com/azikar24/WormaCeptor/compare/v3.4.0...v3.4.1) (2026-10-17)


### Bug Fixes

* **viewer:** read strings through LocalResources for compose 1.10 lint ([b3644bc](https://github.com/azikar24/WormaCeptor/commit/b3644bc43270f78eb2c95e79de2d1e6dc3b8efad))


### Build

* **api:** regenerate api dumps for kotlin 2.4 codegen ([71d46ee](https://github.com/azikar24/WormaCeptor/commit/71d46eede5759184af89008a5b615d3010883cf0))
* **deps:** bump actions/checkout from 4 to 7 ([76dc2d3](https://github.com/azikar24/WormaCeptor/commit/76dc2d3272e67bb1c20c0f16c3df614e42ce78d5))
* **deps:** bump actions/checkout from 4 to 7 ([f4434f0](https://github.com/azikar24/WormaCeptor/commit/f4434f00fe1020d98bf42473cfe82b57ca565516))
* **deps:** bump actions/setup-java from 4 to 6 ([81ae58e](https://github.com/azikar24/WormaCeptor/commit/81ae58ec9312fad3c7f2dcb22449d8b3bebdfc3d))
* **deps:** bump actions/upload-artifact from 4 to 7 ([a3fef06](https://github.com/azikar24/WormaCeptor/commit/a3fef06fa2d54e0ef6537f6a03e7a77fbe5294d4))
* **deps:** bump agp to 8.13.2, gradle to 8.13, pin r8 9.1.56 ([d08497c](https://github.com/azikar24/WormaCeptor/commit/d08497ce989ee4e3baa2905b14c4a6e832717f38))
* **deps:** bump androidx.paging:paging-runtime from 3.3.6 to 3.5.1 ([f9b006f](https://github.com/azikar24/WormaCeptor/commit/f9b006fce7eb22a29f52fd639b1e15404542f4eb))
* **deps:** bump com.google.firebase:firebase-messaging-ktx from 24.1.0 to 24.1.2 ([8411735](https://github.com/azikar24/WormaCeptor/commit/8411735717973ba5d0c1fbfeabd86ad352ad3645))
* **deps:** bump de.mannodermaus.android-junit5 from 1.11.0.0 to 2.0.1 ([69d2095](https://github.com/azikar24/WormaCeptor/commit/69d2095d458e53a58e383a4af5034d6baf653624))
* **deps:** bump dependency-analysis to 3.19.2 ([6490ea7](https://github.com/azikar24/WormaCeptor/commit/6490ea73b2541246f620039fb17316abb8754c6b))
* **deps:** bump gitleaks/gitleaks-action from 2 to 3 ([ca68b42](https://github.com/azikar24/WormaCeptor/commit/ca68b42c5618b3f01795296d2cd84d97eecb94a5))
* **deps:** bump gradle/actions from 3 to 6 ([5006662](https://github.com/azikar24/WormaCeptor/commit/5006662b54390b3b2f4d173c5bcef5c45d713269))
* **deps:** bump io.mockk:mockk from 1.13.12 to 1.14.11 ([db3cce3](https://github.com/azikar24/WormaCeptor/commit/db3cce31e5f3e081a54b69e295ab8006e83d54b3))
* **deps:** bump kotlin to 2.4.20 ([7d70ac5](https://github.com/azikar24/WormaCeptor/commit/7d70ac5f8b3aa89c7db1b30e505040dc5ac4a604))
* **deps:** bump kotlinx-serialization-json from 1.6.3 to 1.11.0 ([ecc06ea](https://github.com/azikar24/WormaCeptor/commit/ecc06ea2b31d8dc763678ccc5f5a528566fd1b44))
* **deps:** bump kover to 0.9.11 and move to the kover { reports } dsl ([4420000](https://github.com/azikar24/WormaCeptor/commit/442000006ebcb0c134a746ec115a882c244073b9))
* **deps:** run lint 9.1.1 so it can read kotlin 2.4 metadata ([fde674a](https://github.com/azikar24/WormaCeptor/commit/fde674ab497dd0bcca7e4ef66e6d6e0b034249e1))
* **deps:** upgrade kotlin to 2.4.20 and the toolchain that depends on it ([650e056](https://github.com/azikar24/WormaCeptor/commit/650e056c61e3c9ea895090883c4bd0f8f265808a))
* **mcp:** publish the server to the official MCP Registry ([3e49275](https://github.com/azikar24/WormaCeptor/commit/3e492751e523da358e1349a461e7be299000c6bd))
* **mcp:** publish the server to the official MCP Registry ([64d52ce](https://github.com/azikar24/WormaCeptor/commit/64d52ce525069e6e633ac47f0fa9d8a05049ee80))

## [3.4.0](https://github.com/azikar24/WormaCeptor/compare/v2.3.2...v3.4.0) (2026-10-07)


### Features

* **mcp:** add export_curl and replay_transaction tools ([5445a0a](https://github.com/azikar24/WormaCeptor/commit/5445a0a861d1be7b16207ce800de916b2ef49411))
* **mcp:** add get_timeline tool ([8545c6e](https://github.com/azikar24/WormaCeptor/commit/8545c6ec0f592af00564e6540e189e1b997892a6))
* **mcp:** add MCP device server and bridge ([70f7a49](https://github.com/azikar24/WormaCeptor/commit/70f7a4995113a8dd5e6eaf239f30b1e4818830c6))
* **mcp:** add mock rule tools ([267680a](https://github.com/azikar24/WormaCeptor/commit/267680a8cfcc33658fc85fb99256951ce859fee5))
* **mcp:** add set_monitoring to start and stop cpu, memory and fps monitors ([700adc6](https://github.com/azikar24/WormaCeptor/commit/700adc61b9bb4d0b145db128c2c43cddf5892510))
* **mcp:** add tool annotations and protocol negotiation ([f395d42](https://github.com/azikar24/WormaCeptor/commit/f395d42e3d44195a23768bc93c8c3adf71f39c6c))
* **mcp:** add wait_for_transaction and wait_for_crash tools ([5a459ee](https://github.com/azikar24/WormaCeptor/commit/5a459ee5b4a81adbaa47e2ce3f7abd674ae75526))
* **mcp:** add wormaceptor-mcp npx launcher for the bridge ([a4744a3](https://github.com/azikar24/WormaCeptor/commit/a4744a36ed5f1a124a025faac8a9f9eff41949bb))
* **mcp:** diagnose frozen or dead apps and add bring_app_to_front ([0bc80d6](https://github.com/azikar24/WormaCeptor/commit/0bc80d6aef18d86180a5e095807a84f2c0201b93))
* **mcp:** find adb in the Android SDK when it isn't on PATH ([b5a46f1](https://github.com/azikar24/WormaCeptor/commit/b5a46f1cd001721953ece3fd5a98534e112f8923))
* **mcp:** MCP device server and bridge for AI agents ([17094d1](https://github.com/azikar24/WormaCeptor/commit/17094d17b9fb203d0faf730d731a3ab15affbfb7))
* **mcp:** read tail_logs from adb logcat instead of in-app capture ([bfa5d88](https://github.com/azikar24/WormaCeptor/commit/bfa5d8806dcf26729fd1f679d0e51ac596a2a8bd))
* **mcp:** redact secret-looking preference and file values ([8e43e09](https://github.com/azikar24/WormaCeptor/commit/8e43e0938d59416f697b294465bcb069f4b13ff3))
* **mcp:** show timestamps as local ISO time with relative age ([3f865c9](https://github.com/azikar24/WormaCeptor/commit/3f865c9ee1f705c774013acb7525abd899d8ae9e))


### Bug Fixes

* **api:** let host apps construct McpConfig ([33a8c45](https://github.com/azikar24/WormaCeptor/commit/33a8c45397cc7aaa704aad5ce65391778f723ddc))
* **build:** publish POMs under the MIT license ([1c969d5](https://github.com/azikar24/WormaCeptor/commit/1c969d53bbe507574d156396b28984edb12c4c14))
* **mcp:** align bridge tools with device-server routes and params ([ce8adbf](https://github.com/azikar24/WormaCeptor/commit/ce8adbf63c8b856ce5cd1b52d2b58cab2a2aa7db))
* **mcp:** always send jsonrpc 2.0 in bridge responses ([7d8d2bb](https://github.com/azikar24/WormaCeptor/commit/7d8d2bb93815300c7d6136d8c70498dd94fcde7d))
* **mcp:** answer the MCP handshake before the device connects ([879769f](https://github.com/azikar24/WormaCeptor/commit/879769f042388c91f1f82a8ee0718ad259292710))
* **mcp:** apply McpConfig before the server auto-starts ([ea4559a](https://github.com/azikar24/WormaCeptor/commit/ea4559a77861ebe9bb115f2926b11836a7c3dea8))
* **mcp:** auto-start the device server and connect the event stream ([33a42e2](https://github.com/azikar24/WormaCeptor/commit/33a42e220fc692e9c46ca6f9a2df24fcc9b65542))
* **mcp:** avoid ConcurrentHashMap.newKeySet on API 23 ([c435690](https://github.com/azikar24/WormaCeptor/commit/c43569052e34ba05c143154e0252925a33c51f25))
* **mcp:** cap tool output at 20,000 characters and lower list defaults ([adaad4f](https://github.com/azikar24/WormaCeptor/commit/adaad4f8714256b198dc57c563f41e00efe76564))
* **mcp:** correct the tool count and show readable times in new tools ([6792489](https://github.com/azikar24/WormaCeptor/commit/679248915018b3aa3e6b29bca9f504329462f225))
* **mcp:** filter transactions by query and return bodies as JSON ([518df27](https://github.com/azikar24/WormaCeptor/commit/518df27f2582c02aabb95d82b1c04d34e016eecd))
* **mcp:** find the app's package via adb if it starts frozen ([026ac34](https://github.com/azikar24/WormaCeptor/commit/026ac34f05265a4d2d330dc54c06ba391bd66ed6))
* **mcp:** flag tool validation and server errors with isError ([4c29f38](https://github.com/azikar24/WormaCeptor/commit/4c29f382884ed39e7c27b630fe359dfbc01d3bce))
* **mcp:** make storage, inspection and clear routes return data ([35248dd](https://github.com/azikar24/WormaCeptor/commit/35248dd3cfaaaff7f36e57a5c7c44c5efa074957))
* **mcp:** merge adb warnings and errors into the timeline ([9db258f](https://github.com/azikar24/WormaCeptor/commit/9db258f8d8805f7ccdaeaba2d53076113b809693))
* **mcp:** reconnect the bridge when the device server goes away ([4e35862](https://github.com/azikar24/WormaCeptor/commit/4e35862dce42af8697f8f7947e5bea3122946789))
* **mcp:** render the device server's fields in tool output ([5c98e9e](https://github.com/azikar24/WormaCeptor/commit/5c98e9ef4ed9c0fac8d46cd4eb9c421ec29e7986))
* **mcp:** report device-server errors instead of false success ([ee65a86](https://github.com/azikar24/WormaCeptor/commit/ee65a86e0ce0ec6cbfb0946c30b2641bbbd7d392))
* **mcp:** return live data from monitors, logs and dependencies ([d2d69a4](https://github.com/azikar24/WormaCeptor/commit/d2d69a4d4a2ea093b074cd55229f49700c7e3056))


### Refactoring

* **mcp:** share DTOs between bridge and device server ([3e2c27f](https://github.com/azikar24/WormaCeptor/commit/3e2c27f3786d44915b26d3dfcde50e4a63ff959e))


### Build

* **lint:** ignore ktor package check in device-server ([48b66a2](https://github.com/azikar24/WormaCeptor/commit/48b66a20aaf2fed08ab15a3ff8b838662069b4f5))
* **mcp:** publish the device server and stamp the release version ([0acd500](https://github.com/azikar24/WormaCeptor/commit/0acd500a4ea2bfadf8f750067402915503db80f2))


### CI/CD

* **release:** attach the MCP bridge jar and publish the npx launcher ([f9dd499](https://github.com/azikar24/WormaCeptor/commit/f9dd4997a46b5120b31f68e5a959deac3e2d8f3e))
* **release:** publish the npx launcher with npm trusted publishing ([b87bae3](https://github.com/azikar24/WormaCeptor/commit/b87bae32104348e368102919591e13762c85ed6f))


### Chores

* release 3.4.0 ([ef949ce](https://github.com/azikar24/WormaCeptor/commit/ef949ceb782db2b04f83e465c9348bbfdb98b83b))

## [2.3.2](https://github.com/azikar24/WormaCeptor/compare/2.3.1...v2.3.2) (2026-10-06)


### Features

* **app:** add a Seed DataStore test tool ([10a574c](https://github.com/azikar24/WormaCeptor/commit/10a574ca12277017048157fcc60faaf80a75e30a))
* **demo:** add sample services and stress tools to exercise inspectors ([93b21f6](https://github.com/azikar24/WormaCeptor/commit/93b21f656c3f9f63718e548b2b6195c3a2ca6a1a))
* neutral-first redesign, host-safety fixes, Copy URL and Add to Mock ([cba1cb1](https://github.com/azikar24/WormaCeptor/commit/cba1cb1eef24f0f9e4e0ab1d54731d2ee039f442))
* **preferences:** add a read-only DataStore inspector tool ([9401b12](https://github.com/azikar24/WormaCeptor/commit/9401b128caee46d91969814ee76d4c33a640a5b2)), closes [#16](https://github.com/azikar24/WormaCeptor/issues/16)
* **ui:** neutral-first design system refresh ([0db162e](https://github.com/azikar24/WormaCeptor/commit/0db162e5db705ac7923847d6fc242feadbfaa9fa))
* **viewer:** copy URL, add to mock, and transaction viewer fixes ([93a3d8f](https://github.com/azikar24/WormaCeptor/commit/93a3d8f532d40bc023b973cde3b5a541cf04a069))


### Bug Fixes

* **api:** address pre-merge review of response capture and engines ([a3c5676](https://github.com/azikar24/WormaCeptor/commit/a3c56764086052539bdf19628ac6f5d917e79e46))
* **api:** stream response capture and keep debug parts out of release ([f76a5bf](https://github.com/azikar24/WormaCeptor/commit/f76a5bf3f505ddb6e814cc6bde5fb73369eb7c39))
* **app:** demo app permissions, insets and test screens ([9325bd7](https://github.com/azikar24/WormaCeptor/commit/9325bd7167e4ae8ffa85447cfede9128dfe9c490))
* **engine:** correct capture, monitoring and storage engine bugs ([80c3e4d](https://github.com/azikar24/WormaCeptor/commit/80c3e4dce9d9e6a11af83e3e7a6d2db9437e8318))
* **features:** tool screen bug fixes and color cleanup ([249a57d](https://github.com/azikar24/WormaCeptor/commit/249a57dea77b98c19c23d3c94529bcfb5b36f96f))
* **mockrules:** stop double keyboard padding in the rule editor ([6e618d5](https://github.com/azikar24/WormaCeptor/commit/6e618d5c0a7939e9af8f080386d4894840a9adbc))
* **preferences:** load lists without waiting for the search debounce ([26eefe7](https://github.com/azikar24/WormaCeptor/commit/26eefe7703db7ca0c8fb9ea1bad20af30e24324f))
* **release:** let release-please bump the published VERSION_NAME ([deab6be](https://github.com/azikar24/WormaCeptor/commit/deab6beda69e3b286c81ef5bc94af5243ca98def))
* **ui:** replace ExposedDropdownMenuBox to survive Material3 1.4 ([8d922d7](https://github.com/azikar24/WormaCeptor/commit/8d922d730e0210b497221c1521047c06ccfe09bf)), closes [#17](https://github.com/azikar24/WormaCeptor/issues/17)
* **viewer:** address pre-merge review of viewer, PDF and mock editor ([4187789](https://github.com/azikar24/WormaCeptor/commit/41877892c120dcba1428336d68512b570447d02f))
* **viewer:** respect side and bottom system bar insets ([445fdde](https://github.com/azikar24/WormaCeptor/commit/445fddeb725c6a89eaa1d4ecea235dff89cc8d55)), closes [#18](https://github.com/azikar24/WormaCeptor/issues/18)


### CI/CD

* add dependabot config ([8da5925](https://github.com/azikar24/WormaCeptor/commit/8da59251d3493dbe0b6d03cb458673f38023ec05))
* auto-release pipeline on merge to master ([027592a](https://github.com/azikar24/WormaCeptor/commit/027592a124a6a84c658e56d8fa83aeb7a9de22c9))
* auto-release pipeline on merge to master ([683bba0](https://github.com/azikar24/WormaCeptor/commit/683bba0d5ec7f26ab470aad22e4e51f62acdf7d9))
* **coverage:** drop koverHtmlReport from CI to avoid empty-module crash ([9ffd0aa](https://github.com/azikar24/WormaCeptor/commit/9ffd0aa9be78311c16d7ea977382c6cf55983a8f))


### Chores

* release 2.3.2 ([26f192f](https://github.com/azikar24/WormaCeptor/commit/26f192ffa33d7e7b4c7ad85042000c9610af8044))

## [2.2.1] - 2025-03-24

### Added
- Ktor client plugin for native Ktor HTTP client support
- WebSocket monitoring with frame inspection
- WebView monitoring for page loads and sub-resources
- Performance overlay with real-time FPS, Memory, and CPU metrics
- SQLite database browser with custom query execution
- SharedPreferences inspector and editor
- EncryptedSharedPreferences inspector
- File system browser
- Leak detection for Activities and Fragments (LeakCanary integration)
- Thread violation detection (StrictMode)
- Crash reporting with stack traces
- Push notification simulator
- Location simulator for GPS mocking
- Rate limiter with network presets (2G, 3G, 4G, WiFi)
- Crypto toolkit (AES, RSA, hashing, encoding)
- Console logs viewer
- Device info inspector
- Loaded libraries inspector
- Dependencies inspector
- Feature toggle system with 20 toggleable features
- Deep link navigation to every tool (wormaceptor:// scheme)
- Extension provider system for custom metadata
- Data redaction (headers, JSON values, XML values, regex patterns)

### Changed
- Complete rewrite with Clean Architecture (50+ modules)
- 100% Kotlin + Jetpack Compose UI
- Koin dependency injection
- ArchUnit-enforced architecture boundaries

## [1.0.0] - Initial Release

### Added
- Basic HTTP/HTTPS network inspection
- OkHttp interceptor
- Transaction list and detail views

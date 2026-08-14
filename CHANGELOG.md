# Changelog

## [0.2.0](https://github.com/dev-tonholo/svgo-kt/compare/4.0.1-0.1.0...4.0.1-0.2.0) (2026-08-14)


### Features

* add editor namespaces and presentation attribute constants to Collections ([b294b7d](https://github.com/dev-tonholo/svgo-kt/commit/b294b7dcc8340193eae0d9f5e0b2d93f7fc6a642))
* add jvm support ([26d61c5](https://github.com/dev-tonholo/svgo-kt/commit/26d61c572a4de5eebf370ea42442a76acfd79e70))
* add mapNodesToParents utility for parent lookup map ([1eda595](https://github.com/dev-tonholo/svgo-kt/commit/1eda5959742293065455e4bddb9bf9fe3f59655f))
* add MergeStyles plugin ([46e1202](https://github.com/dev-tonholo/svgo-kt/commit/46e120233b6b266d5d43f6cebfbeb5db840ef0fa))
* add querySelector/querySelectorAll stubs (blocked on kss) ([04f0cd8](https://github.com/dev-tonholo/svgo-kt/commit/04f0cd8bca0bc2046a36a597c8eed1cef27ae1e4))
* add StringfySvg ([fb048fa](https://github.com/dev-tonholo/svgo-kt/commit/fb048fa45d4d509a6bed71cc14187994020bcb3c))
* added plugin foundation. ([321d55e](https://github.com/dev-tonholo/svgo-kt/commit/321d55ec62e033e28b72e1e2fca90967512f8878))
* **convertPathData:** add absolute-to-relative conversion and core optimizations ([28ec331](https://github.com/dev-tonholo/svgo-kt/commit/28ec3317f393cc048a82b8791711f148621f5537))
* create svgo parser ([e39868f](https://github.com/dev-tonholo/svgo-kt/commit/e39868fef38815d409b196cdbf7e4c40ad6f4a41))
* implement all remaining plugins (Phase 6 + Phase 7) ([e487e86](https://github.com/dev-tonholo/svgo-kt/commit/e487e8652320473dbe3210cdceba7475e69fee49))
* implement cleanupNumericValues, removeUselessStrokeAndFill, removeHiddenElems plugins ([f6a3ff6](https://github.com/dev-tonholo/svgo-kt/commit/f6a3ff6470c9067502bb23cb9f36b5b425075ffd))
* implement convertColors, removeUselessDefs, removeUnknownsAndDefaults plugins ([9cd1bd5](https://github.com/dev-tonholo/svgo-kt/commit/9cd1bd51f57b848f210b15f752094932842369f8))
* implement convertShapeToPath and mergePaths plugins ([d094b4d](https://github.com/dev-tonholo/svgo-kt/commit/d094b4df13a4883831f387529b8aa1baa17c32ca))
* implement convertTransform plugin ([b06a82f](https://github.com/dev-tonholo/svgo-kt/commit/b06a82f82eb898af516872ed25690eca80f04e13))
* implement CSS selector functions using kss library ([116deb2](https://github.com/dev-tonholo/svgo-kt/commit/116deb2b0eef77547ed07ff3c676e856dbb0b7fc))
* implement DataUri encoding (base64, encoded, unencoded) ([7996121](https://github.com/dev-tonholo/svgo-kt/commit/7996121f36f476d8f5031f712f653e0044590876))
* implement moveGroupAttrsToElems and moveElemsAttrsToGroup plugins ([cedaa31](https://github.com/dev-tonholo/svgo-kt/commit/cedaa31d2724fa57b2b7eea8a601cb9c375dc173))
* implement optional plugins (addClassesToSVGElement, removeAttributesBySelector, prefixIds, removeOffCanvasPaths, convertStyleToAttrs, cleanupListOfValues, reusePaths, convertOneStopGradients) ([ad8e2f4](https://github.com/dev-tonholo/svgo-kt/commit/ad8e2f4d12a7c7f0f1ec98cded8d434cabb6ddd5))
* implement plugin visit engine ([a86af9d](https://github.com/dev-tonholo/svgo-kt/commit/a86af9d66fbfec89d8d72976088353eb88622471))
* implement removal plugins (removeDoctype, removeXMLProcInst, removeComments, removeMetadata, removeDesc) ([16ee86d](https://github.com/dev-tonholo/svgo-kt/commit/16ee86dbe994082be2bce923dc13a816e95ba9b7))
* implement removeDeprecatedAttrs and cleanupEnableBackground plugins ([085d236](https://github.com/dev-tonholo/svgo-kt/commit/085d236af9fc762153a416ae4b07ba91af4c409a))
* implement removeEditorsNSData and removeNonInheritableGroupAttrs plugins ([4b28c3f](https://github.com/dev-tonholo/svgo-kt/commit/4b28c3fa6eeea4260f21a0fe141538c70e2b9657))
* implement removeEmptyContainers and collapseGroups plugins ([39f53bf](https://github.com/dev-tonholo/svgo-kt/commit/39f53bf57e9930b5bae118c80374ac1e7ca33e28))
* implement simple plugins (removeEmptyAttrs, removeEmptyText, cleanupAttrs, convertEllipseToCircle) ([b7ca6d1](https://github.com/dev-tonholo/svgo-kt/commit/b7ca6d1f9ad0f28ae921ddf42f03b069d146620c))
* implement sorting and namespace plugins (sortAttrs, sortDefsChildren, removeUnusedNS) ([ea51865](https://github.com/dev-tonholo/svgo-kt/commit/ea51865bbb6d3723889609b2919984f0e687c74e))
* implement SVG transform parsing, math, and stringification ([f8e4cda](https://github.com/dev-tonholo/svgo-kt/commit/f8e4cda6899f32e33b6a42310934a2ff9568ad48))
* **kmp:** add wasmJs target for KMP browser consumers ([37493c1](https://github.com/dev-tonholo/svgo-kt/commit/37493c164f3c29718bbdd8e927b51a6f2681bdb3))
* **path:** implement path data types, parser, stringifier, and helpers ([7e2152d](https://github.com/dev-tonholo/svgo-kt/commit/7e2152dfd63932f4e126400e8efa4d3becb0a4b9))
* **plugins:** implement applyTransforms to bake transform into path data ([1751c9a](https://github.com/dev-tonholo/svgo-kt/commit/1751c9ac5660159aa93719fdf96da804562a0603))
* **plugins:** implement removeElementsByAttr and removeXlink ([2ace591](https://github.com/dev-tonholo/svgo-kt/commit/2ace59137cd1b8811570b1e3ff6809adc334bb50))
* register 27 implemented plugins in PresetDefault ([e8e3d6a](https://github.com/dev-tonholo/svgo-kt/commit/e8e3d6a0c4f094fb6189b6a17d294420a8ed24d5))
* sax xml reader implementation ([b10c61d](https://github.com/dev-tonholo/svgo-kt/commit/b10c61db2384590b7dd64c315e8952b452d3a0ac))
* **skills:** add unit-test-author, svg-spec, and self-review AI skills with reference docs ([9307c37](https://github.com/dev-tonholo/svgo-kt/commit/9307c3708102dfe310c5376b9433a54534b2e3eb))
* **style:** support value matching in includesAttrSelector ([fda15dc](https://github.com/dev-tonholo/svgo-kt/commit/fda15dc40fcba12014ff545a20f3c7681be0176a))
* svgo implementation ([b5bbee3](https://github.com/dev-tonholo/svgo-kt/commit/b5bbee3f74ec19d999b889207047a4e70fddd48a))


### Bug Fixes

* address code review findings ([69e6d26](https://github.com/dev-tonholo/svgo-kt/commit/69e6d26be002a74e5b66332bdb1dca9e729edbae))
* attribName and attribValue being duplicated ([2a4a83b](https://github.com/dev-tonholo/svgo-kt/commit/2a4a83b64b986b698a69eb6c32dce62fd7e68cc3))
* **ci:** track gradle-wrapper.jar so ./gradlew runs on checkout ([242fb67](https://github.com/dev-tonholo/svgo-kt/commit/242fb678f73c321c1e60b86e0f31eb6aa911b6cf))
* **cleanupIds:** detach attribute snapshot before mutation ([fe9d8e4](https://github.com/dev-tonholo/svgo-kt/commit/fe9d8e4d1c1032f9dd6bcb095d542188f994b4de))
* **convertPathData:** fix all 6 remaining integration test failures ([20d50ad](https://github.com/dev-tonholo/svgo-kt/commit/20d50ad9c02f2e639914d0b03ac6669de34976ce))
* **convertTransform:** implement full matrix decomposition and optimization pipeline ([c57b86a](https://github.com/dev-tonholo/svgo-kt/commit/c57b86a907f12f9d2adac01faef9f09b1bbfd7c9))
* **inlineStyles:** fix fixtures 13, 14, 15, 27 ([87ce931](https://github.com/dev-tonholo/svgo-kt/commit/87ce9313d165759460b82399c3c0be6a702660af))
* **inlineStyles:** preserve class/id refs from remaining selectors and minify rebuilt selectors ([32482bf](https://github.com/dev-tonholo/svgo-kt/commit/32482bf236e8927aa23977d7895ea6e345954a36))
* **parser:** make SvgoParser reusable across multiple parseSvg calls ([b5560d3](https://github.com/dev-tonholo/svgo-kt/commit/b5560d339facd5e9b2960f78a251f281bce5c75e))
* **parser:** subscribe collector before writer to avoid dropped SAX events ([8fd6b54](https://github.com/dev-tonholo/svgo-kt/commit/8fd6b54056e8756fe276acb2fa8ef62d1c4eabc5))
* **plugins:** escape ] and } literals in regex patterns for JS compatibility ([3d29a22](https://github.com/dev-tonholo/svgo-kt/commit/3d29a22ac2f934eeb8565450a8f8b8305bba800c))
* **plugins:** fix 6 plugins with failing integration fixtures ([3f87c47](https://github.com/dev-tonholo/svgo-kt/commit/3f87c47974d8907c73415c801691091daf9ebb3b))
* **plugins:** fix cleanupIds and collapseGroups integration failures ([32a91b2](https://github.com/dev-tonholo/svgo-kt/commit/32a91b2a839e17880ab2d412f2a55c9f60e23e9a))
* **plugins:** fix convertColors, convertStyleToAttrs, cleanupEnableBackground ([b8fe0bc](https://github.com/dev-tonholo/svgo-kt/commit/b8fe0bc7ab6de8874d026da2db2ccb0113f481dc))
* **plugins:** fix mergePaths and minifyStyles integration fixtures ([50dc124](https://github.com/dev-tonholo/svgo-kt/commit/50dc124b79cfa1b71180f8131f8c5e8d7a1f16b5))
* **plugins:** fix mergePaths, removeEmptyContainers, removeHiddenElems ([f689ed1](https://github.com/dev-tonholo/svgo-kt/commit/f689ed1033f3ca51c59a06349d532c956d6c010a))
* **plugins:** fix prefixIds, reusePaths, and convertOneStopGradients ([b542c94](https://github.com/dev-tonholo/svgo-kt/commit/b542c941cd5200332905082cb966c41774a0c520))
* **plugins:** fix regressions in addAttributesToSVGElement, convertTransform, and removeDoctype ([bfcde2b](https://github.com/dev-tonholo/svgo-kt/commit/bfcde2bd79992821fb57dcc1d42ab4d4787f2e71))
* **plugins:** fix removeUselessStrokeAndFill, removeHiddenElems, removeUnknownsAndDefaults ([ab6580c](https://github.com/dev-tonholo/svgo-kt/commit/ab6580ca1dfd593a5656be733c636dbd3c516757))
* **plugins:** fix sortAttrs comparator and stringifier empty-attribute output ([3376fba](https://github.com/dev-tonholo/svgo-kt/commit/3376fba59565acbfd6aa2bff82bf61942421e902))
* **plugins:** replace java.util.IdentityHashMap with common-safe reference-identity map ([7f035ed](https://github.com/dev-tonholo/svgo-kt/commit/7f035ed4da189122e0f9269ffd92795b86848639))
* **plugins:** replace JVM-only toBigDecimal with common-safe number formatter ([bdd864e](https://github.com/dev-tonholo/svgo-kt/commit/bdd864e67c44a52cf687160ffca41d0a156ad161))
* **plugins:** replace PluginParams companion invoke with top-level factory ([af24ca3](https://github.com/dev-tonholo/svgo-kt/commit/af24ca3f2a65ca6e46a3df429c6a5edabfcc2f48))
* procInstBody being updated instead of procInstName ([cf7abc9](https://github.com/dev-tonholo/svgo-kt/commit/cf7abc949509020b05978a945cb14cc7ac9fb03b))
* regex wrongly matched ([cedea61](https://github.com/dev-tonholo/svgo-kt/commit/cedea61a0a2210f01c1eedb146807cd9130f16e0))
* resolve detekt violations in non-SAX source files ([400bca2](https://github.com/dev-tonholo/svgo-kt/commit/400bca2a4923d972fc18e36328f57c8eec3af991))
* resolve pre-existing compilation errors in ComputeStyle and ApplyTransforms ([fbdb167](https://github.com/dev-tonholo/svgo-kt/commit/fbdb167fe7be63a8292c752d526cca08c9e30d78))
* **sortAttrs:** copy attribute entries before clearing to avoid ConcurrentModificationException ([fd1f294](https://github.com/dev-tonholo/svgo-kt/commit/fd1f294951064443fba1f11a82e3035c00d8f4ea))
* **stringifier:** correct text node whitespace and instruction/doctype output ([e899ff0](https://github.com/dev-tonholo/svgo-kt/commit/e899ff0319c0415886ad3b260aaa7da4b983ea11))
* **stylesheet:** fix Specificity IOOB by correcting component count ([30c0766](https://github.com/dev-tonholo/svgo-kt/commit/30c0766b4d4cf66c64b29b4d89be992a091aea41))
* **test:** adjust test fixtures to work with current parser limitations ([ef757c5](https://github.com/dev-tonholo/svgo-kt/commit/ef757c5eefec92ccc06dc1a1dec1c7bdb3625f50))
* **test:** route integration tests through Svgo.optimize() pipeline ([9a788f7](https://github.com/dev-tonholo/svgo-kt/commit/9a788f7140a2427882ada3d2ffe425cda915c29b))
* **test:** use Svgo.optimize() pipeline in integration test harness ([ca4ba51](https://github.com/dev-tonholo/svgo-kt/commit/ca4ba51dc3b658a0771c952ba55d17d0abff1619))
* text entity not processed correctly ([31c5d31](https://github.com/dev-tonholo/svgo-kt/commit/31c5d318a43be76086a4640bd9566792ae561267))
* text node not emitted ([ddd0539](https://github.com/dev-tonholo/svgo-kt/commit/ddd0539f538a9c4707e39877850424626c38d825))


### Performance Improvements

* **benchmarks:** add compare-bench.sh for side-by-side svgo comparison ([ff330de](https://github.com/dev-tonholo/svgo-kt/commit/ff330dee193a1467edcbd43ce5f39a6279cd1779))
* **benchmarks:** add JMH-driven optimize() benchmarks vs upstream svgo ([9323582](https://github.com/dev-tonholo/svgo-kt/commit/932358279cbe6ccfb6b3010b9cc8f5215d6927ba))
* **benchmarks:** emit per-target comparison and add JMH gc profile ([814b622](https://github.com/dev-tonholo/svgo-kt/commit/814b622180b5f933490ebcf23abf956d42e0c67d))
* **benchmarks:** run optimize() benchmark on every host target ([14dc785](https://github.com/dev-tonholo/svgo-kt/commit/14dc785f03de05494a923409a7bb52d787bcf604))
* **css-select:** bail out on non-standard combinators before parsing ([3b652ff](https://github.com/dev-tonholo/svgo-kt/commit/3b652ffdb1cced1b9f2db0043c11716c34729068))


### Refactors

* clean up CssSelectParser, document kss workarounds ([1796717](https://github.com/dev-tonholo/svgo-kt/commit/17967178f40dab088d23d2fa37337c3609b37c01))
* **inlineStyles:** drop private regex helper for includesAttrSelector ([2d97066](https://github.com/dev-tonholo/svgo-kt/commit/2d970669ed7d06c1ac989fb79ea40e77a1fc56db))
* introduce PluginConfig sealed type replacing List&lt;Any&gt; for plugin config ([77e2be2](https://github.com/dev-tonholo/svgo-kt/commit/77e2be24e9ace78ff15367223c07b0c9d76f237d))
* remove CSS selector fallback parser, use kss 1.0.1 directly ([c7d2fb5](https://github.com/dev-tonholo/svgo-kt/commit/c7d2fb5eb05d088521df37c4f8b4b9efee573da9))
* **removeDeprecatedAttrs:** extract attrs via kss selector parser ([c4ba411](https://github.com/dev-tonholo/svgo-kt/commit/c4ba411df0727231c5cc726a9db17f0777f42d77))
* **style:** parse selectors with kss in includesAttrSelector ([3bc79c5](https://github.com/dev-tonholo/svgo-kt/commit/3bc79c59b3bf8322f28499297e6da8e370eb6ef4))
* support both Int and String indent in StringifyOptions ([a77e8ad](https://github.com/dev-tonholo/svgo-kt/commit/a77e8ad16a5ee2ff4b593fa7eb01c6ab010636d5))
* **version:** split compound version into svgo-upstream and svgokt entries ([b7cca27](https://github.com/dev-tonholo/svgo-kt/commit/b7cca27a20d41f0cdf69164b216706c44ef0f99f))
* **xast:** drop kss workarounds using kss 1.0.2 APIs ([2629df5](https://github.com/dev-tonholo/svgo-kt/commit/2629df5190255e71837a4c08890f4b0d1b206a1a))


### Build System

* add Maven Central snapshots repo ([9cc2abf](https://github.com/dev-tonholo/svgo-kt/commit/9cc2abffb639c13fedac3f32168693027169581c))
* introduce build-logic convention plugins for KMP, publication, fixtures and detekt ([9002fdc](https://github.com/dev-tonholo/svgo-kt/commit/9002fdc61c84d523844b31310066f91b8d80938a))


### Continuous Integration

* add CodeQL static analysis workflow ([007cfc2](https://github.com/dev-tonholo/svgo-kt/commit/007cfc216d00d2c339c8ee91dc692c027f8efa02))
* add GitHub Actions for build, release, and upstream SVGO tracking ([7dcd979](https://github.com/dev-tonholo/svgo-kt/commit/7dcd9793fe46584605d73f72e0d13fca3167133a))
* add release-please for automated changelog and version bumps ([dc19884](https://github.com/dev-tonholo/svgo-kt/commit/dc19884cc880389e9705f3dbc8b5021627d2864d))
* **coverage:** add Kover with HTML/XML reports and a CI gate ([4cb2110](https://github.com/dev-tonholo/svgo-kt/commit/4cb21105a7bb59d51cc1ad66d582745a6a221494))
* expand test matrix to cover macosArm64 and mingwX64 ([60c18e4](https://github.com/dev-tonholo/svgo-kt/commit/60c18e4ad4be01d162d34b16cd479b713a7337b5))
* **publish:** add GitHub Packages, auto-release Central, and dispatch trigger ([24ede2b](https://github.com/dev-tonholo/svgo-kt/commit/24ede2b3d76da75afedefdadc578c93b2b8347c7))
* **svgo-upgrade:** create an Epic-typed issue instead of a milestone ([4e12527](https://github.com/dev-tonholo/svgo-kt/commit/4e1252728230dbf8f33b2ced93f814947b18ecda))


### Documentation

* add CONTRIBUTING and CODE_OF_CONDUCT ([889d962](https://github.com/dev-tonholo/svgo-kt/commit/889d96221c1ef0c64199e9a8df874c416871214b))
* add Dokka API site published to GitHub Pages ([c62087a](https://github.com/dev-tonholo/svgo-kt/commit/c62087a453c6d39ddc56d0da2314cd845ae6e85c))
* add README file ([826d5f3](https://github.com/dev-tonholo/svgo-kt/commit/826d5f3afb2833dac422827633bddc1dc336e4d8))
* add SECURITY policy ([fbf63f8](https://github.com/dev-tonholo/svgo-kt/commit/fbf63f8da1132f26a528b2b7ecae4771ca8e103c))
* **readme:** add CI, Maven Central, license and Kotlin version badges ([0ba4e2e](https://github.com/dev-tonholo/svgo-kt/commit/0ba4e2ebfb139bda41923ecfd19e3847204b602a))
* **readme:** add usage, versioning and installation guides ([e3fc9a1](https://github.com/dev-tonholo/svgo-kt/commit/e3fc9a12e0439778db25304562083f7ce59bffc7))
* **samples:** add README documenting how to run each target ([9476831](https://github.com/dev-tonholo/svgo-kt/commit/94768312387f4529fbe2e09de5b8ae530eb2cb3d))

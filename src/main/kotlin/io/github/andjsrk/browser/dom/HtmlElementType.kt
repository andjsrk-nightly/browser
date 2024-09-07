package io.github.andjsrk.browser.dom

/**
 * Indicates the type of element.
 * Note that the enum is necessary although separate interfaces exist for elements,
 * since some interfaces are shared between some elements.
 */
enum class HtmlElementType {
    // 4.1 The document element
    Html,

    // 4.2 Document metadata
    Head,
    Title,
    Base,
    Link,
    Meta,
    Style,

    // 4.3 Sections
    Body,
    Article,
    Section,
    Nav,
    Aside,
    H1,
    H2,
    H3,
    H4,
    H5,
    H6,
    Hgroup,
    Header,
    Footer,
    Address,

    // 4.4 Grouping content
    P,
    Hr,
    Pre,
    Blockquote,
    Ol,
    Ul,
    Menu,
    Li,
    Dl,
    Dt,
    Dd,
    Figure,
    Figcaption,
    Main,
    Search,
    Div,

    // 4.5 Text-level semantics
    A,
    Em,
    Strong,
    Small,
    S,
    Cite,
    Q,
    Dfn,
    Abbr,
    Ruby,
    Rt,
    Rp,
    Data,
    Time,
    Code,
    Var,
    Samp,
    Kbd,
    Sub,
    Sup,
    I,
    B,
    U,
    Mark,
    Bdi,
    Bdo,
    Span,
    Br,
    Wbr,

    // 4.7 Edits
    Ins,
    Del,

    // 4.8 Embedded content
    Picture,
    Source,
    Img,
    Iframe,
    Embed,
    Object,
    Video,
    Audio,
    Track,
    Map,
    Area,

    // 4.9 Tabular data
    Table,
    Caption,
    Colgroup,
    Col,
    Tbody,
    Thead,
    Tfoot,
    Tr,
    Td,
    Th,

    // 4.10 Forms
    Form,
    Label,
    Input,
    Button,
    Select,
    Datalist,
    Optgroup,
    Option,
    Textarea,
    Output,
    Progress,
    Meter,
    Fieldset,
    Legend,

    // 4.11 Interactive elements
    Details,
    Summary,
    Dialog,

    // 4.12 Scripting
    Script,
    Noscript,
    Template,
    Slot,
    Canvas,

    // 4.13 Custom elements
    Custom,

    // 16 Obsolete features
    // 16.3 Requirements for implementations
    Marquee,
    Frameset,
    Acronym,
    Dir,
    Font,
    Param,
}

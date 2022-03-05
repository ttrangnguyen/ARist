#### ARRAY_ACCESS
Chỉ xét trên variable.

Ví dụ: `object[]` không có `object.field[]`.

#### CAST
Chỉ xét trên variable.

Ví dụ: có `(Class) object` không có `(Class) object.field`.

####  TYPE_LIT
Thêm nếu param có kiểu `java.lang.Class<T>`.

####  NULL_LIT
Thêm nếu param không phải là `primitve type`.

####  METHOD_INVOC
Xét trên variable và trên chính class hiện tại.

Ví dụ: có `object.call(` hoặc `call(`.

####  OBJ_CREATION
Thêm nếu param không phải là primitve type và không phải là kiểu wrap của primitve type và Object.

Ví dụ: không sinh ra `new Integer()`.

####  ARR_CREATION
Chỉ tạo khi array có dimension là `1`.

Ví dụ: có `new arr[]` không có `new arr[][]`.

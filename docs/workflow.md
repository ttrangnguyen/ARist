##STEP 1
1. Tạo đối tượng ProjectParser theo `config`.
2. Tạo đối tượng FileParser theo ProjectParser.
3. Parse theo vị trí `fileParser.parse()`.
4. Sinh param bằng `genNextParams()`, `genParamsAt()`, `genFirstParams()`, kết quả được sinh ra là một `MultiMap` gồm excode - lexcial : 1 - n.
5. Các token dạng `method call` được lưu binding trong  `FileParser.lexMap`.
##STEP 2
```
    Test generator
```

##STEP 3
```
    Predicting/ranking
```
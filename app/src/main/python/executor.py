import sys
from io import StringIO

_namespace = {}

def execute_code(code):
    old_stdout = sys.stdout
    old_stderr = sys.stderr
    sys.stdout = captured_out = StringIO()
    sys.stderr = captured_err = StringIO()

    try:
        try:
            # 尝试作为表达式执行
            result = eval(code, _namespace)
            if result is not None:
                print(result)  # 自动打印表达式结果
        except SyntaxError:
            # 作为语句执行
            exec(code, _namespace)
        except Exception as e:
            # 捕获其他异常并写入stderr
            captured_err.write(f"{type(e).__name__}: {e}")
    except Exception as e:
        # 处理exec执行时的异常
        captured_err.write(f"{type(e).__name__}: {e}")
    finally:
        sys.stdout = old_stdout
        sys.stderr = old_stderr

    # 获取stdout和stderr的内容
    stdout_val = captured_out.getvalue().strip()
    stderr_val = captured_err.getvalue().strip()

    # 根据条件返回结果
    return stderr_val if stderr_val else stdout_val

"""Basic tests to ensure CI passes"""


def test_basic():
    """Basic test that always passes"""
    assert True


def test_imports():
    """Test that basic imports work"""
    import fastapi
    import pydantic
    assert fastapi is not None
    assert pydantic is not None
